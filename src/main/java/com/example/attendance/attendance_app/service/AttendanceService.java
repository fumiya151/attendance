package com.example.attendance.attendance_app.service;

import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.example.attendance.attendance_app.dto.AggregatedAttendanceSummaryDto;
import com.example.attendance.attendance_app.dto.AttendanceRequest;
import com.example.attendance.attendance_app.dto.AttendanceDto;
import com.example.attendance.attendance_app.model.Attendance;
import com.example.attendance.attendance_app.model.DailyAttendanceSummary;
import com.example.attendance.attendance_app.model.Employee;
import com.example.attendance.attendance_app.repository.AttendanceRepository;
import com.example.attendance.attendance_app.repository.DailyAttendanceSummaryRepository;
import com.example.attendance.attendance_app.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final DailyAttendanceSummaryRepository summaryRepository;

    private static final ZoneId JST_ZONE = ZoneId.of("Asia/Tokyo");
    // 日の区切りとなる時刻（午前 9:00 JST）を定義
    private static final LocalTime WORK_DAY_CLOSE_TIME = LocalTime.of(9, 0);

    private final String workSt = "出勤";
    private final String workEd = "退勤";
    private final String breakSt = "休憩開始";
    private final String breakEd = "休憩終了";

    private static final double HOURS_ROUNDING_SCALE = 100.0;
    private static final double MINUTES_IN_HOUR = 60.0;
    private static final String STATUS_FINALIZED = "FINALIZED";
    private static final String STATUS_APPROVED = "APPROVED";

    /**
     * 指定された打刻時刻が属する「勤務日」（Work Date）を計算します.
     *
     * 【機能】
     * 9:00 JSTを日の区切りとして、打刻時刻が前日の勤務に属するか、当日の勤務に属するかを判定します。
     *
     * 【注意事項】
     * 9:00 JSTより前の打刻は前日の勤務日と見なされます。
     *
     * @param stampTime 打刻時刻
     * @return 計算された勤務日（LocalDate）
     */
    private LocalDate getWorkDate(OffsetDateTime stampTime) {
        // JSTでの日付と時刻を取得
        LocalDate date = stampTime.atZoneSameInstant(JST_ZONE).toLocalDate();
        LocalTime time = stampTime.atZoneSameInstant(JST_ZONE).toLocalTime();

        // 打刻時刻が 9:00 JST より前の場合、勤務日は前日となる
        if (time.isBefore(WORK_DAY_CLOSE_TIME)) {
            return date.minusDays(1);
        }
        // 9:00 JST 以降の場合、勤務日は当日となる
        return date;
    }

    /**
     * 次に有効な打刻種別リストを返すメソッドです.
     *
     * 【機能】
     * 最新の打刻種別に基づき、次に打刻可能な種別（出勤、退勤、休憩開始、休憩終了）のリストを返します。
     *
     * 【注意事項】
     * 最初の打刻は必ず「出勤」です。
     *
     * @param employeeId 従業員ID
     * @return 有効な打刻種別リスト
     */
    public List<String> getNextAvailableStampTypes(String employeeId) {
        Optional<AttendanceDto> latestAttendanceOpt = getLatestAttendance(employeeId);

        if (latestAttendanceOpt.isEmpty()) {
            // その勤務日の最初の打刻の場合
            return List.of(workSt);
        }

        String lastStampType = latestAttendanceOpt.get().getStampType();
        switch (lastStampType) {
            case workSt:
                return List.of(workEd, breakSt); // 出勤後は退勤または休憩開始
            case workEd:
                return List.of(workSt); // 退勤後は再度出勤
            case breakSt:
                return List.of(breakEd); // 休憩開始後は休憩終了のみ
            case breakEd:
                return List.of(workEd, breakSt); // 休憩終了後は退勤または休憩開始
            default:
                return List.of(); // 未知のステータス
        }
    }

    /**
     * 勤怠記録を登録するメソッドです.
     *
     * 【機能】
     * 現在時刻で打刻情報をAttendanceテーブルに登録します。打刻種別が「退勤」の場合、日次集計処理（Summaryの更新/登録）をトリガーします。
     *
     * 【注意事項】
     * 従業員IDが存在しない場合はRuntimeExceptionをスローします。
     *
     * @param request       勤怠リクエストDTO
     * @param operatorId 勤怠操作を行った従業員ID (通常は打刻を行った本人)
     * @return 登録された勤怠エンティティ
     */
    @Transactional
    public Attendance recordAttendance(AttendanceRequest request, String operatorId) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + request.getEmployeeId()));

        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        OffsetDateTime stampTime = OffsetDateTime.now(JST_ZONE);
        attendance.setStampTime(stampTime);
        attendance.setStampType(request.getStampType());

        Attendance savedAttendance = attendanceRepository.save(attendance);

        if (request.getStampType().equals(workEd)) {
            // 退勤時刻から「勤務日」を計算する
            LocalDate workDate = getWorkDate(stampTime);
            processCheckoutSummary(request.getEmployeeId(), workDate, operatorId);
        }

        return savedAttendance;
    }

    /**
     * 退勤時の日次集計処理とdaily_attendance_summaryへの登録/更新（UPSERT）を行います.
     *
     * 【機能】
     * 勤務日（Work Date）を跨ぐ範囲の打刻ログを取得し、総労働時間と総休憩時間を計算してSummaryテーブルに保存（更新/挿入）します。
     *
     * 【注意事項】
     * 日の区切り時刻（9:00 JST）に基づき、ログ取得期間が決定されます。
     *
     * @param employeeId 従業員ID
     * @param workDate     勤務日 (9:00締めを考慮して計算済み)
     * @param operatorId 集計を更新した従業員ID
     */
    private void processCheckoutSummary(String employeeId, LocalDate workDate, String operatorId) {
        // 勤務日の 9:00 から、翌日の 9:00 までをログ取得範囲とする
        OffsetDateTime startOfWorkDay = workDate.atTime(WORK_DAY_CLOSE_TIME).atZone(JST_ZONE).toOffsetDateTime();
        OffsetDateTime endOfWorkDay = workDate.plusDays(1).atTime(WORK_DAY_CLOSE_TIME).atZone(JST_ZONE)
                .toOffsetDateTime();

        // DB側でemployeeIdによるフィルタリングを行う
        List<Attendance> logs = attendanceRepository.findByPeriod(employeeId, startOfWorkDay, endOfWorkDay);

        DailyAttendanceSummary calculatedSummary = calculateDailySummary(employeeId, workDate, logs);

        Optional<DailyAttendanceSummary> existingSummaryOpt = summaryRepository.findByEmployeeIdAndWorkDate(employeeId,
                workDate);

        DailyAttendanceSummary summaryToSave = existingSummaryOpt.orElseGet(DailyAttendanceSummary::new);

        if (summaryToSave.getWorkDate() == null) {
            summaryToSave.setEmployeeId(employeeId);
            summaryToSave.setWorkDate(workDate);
            summaryToSave.setStatus("PENDING");
            summaryToSave.setUpdatedById(operatorId);
        }

        summaryToSave.setActualInTime(calculatedSummary.getActualInTime());
        summaryToSave.setActualOutTime(calculatedSummary.getActualOutTime());
        summaryToSave.setTotalWorkMinutes(calculatedSummary.getTotalWorkMinutes());
        summaryToSave.setTotalBreakMinutes(calculatedSummary.getTotalBreakMinutes());
        // 今の計算ロジックでは残業/深夜は未計算のため0を設定
        summaryToSave.setOvertimeMinutes(0);
        summaryToSave.setNightShiftMinutes(0);
        summaryToSave.setCalculatedAt(OffsetDateTime.now(JST_ZONE));

        summaryToSave.setUpdatedById(operatorId);

        summaryRepository.save(summaryToSave);
    }

    /**
     * 打刻ログリストから日次集計オブジェクトを計算・生成します.
     *
     * 【機能】
     * ソートされた打刻ログを巡回し、出勤(workSt)、退勤(workEd)、休憩開始(breakSt)、休憩終了(breakEd)のペアを分析して、
     * 総実労働時間と総休憩時間を分単位で算出します。
     *
     * 【注意事項】
     * 複雑な打刻シーケンス（例：IN -> IN, OUT -> BREAK_ST, BREAK_ED ->
     * OUTがない）は適切に処理されない可能性があります。
     *
     * @param employeeId 従業員ID
     * @param workDate     勤務日
     * @param logs             その日の打刻ログ (9:00締めを考慮した範囲)
     * @return 計算結果が格納されたDailyAttendanceSummaryオブジェクト
     */
    private DailyAttendanceSummary calculateDailySummary(String employeeId, LocalDate workDate, List<Attendance> logs) {
        DailyAttendanceSummary summary = new DailyAttendanceSummary();

        // ログを時系列順にソート（OffsetDateTimeで比較）
        logs.sort(Comparator.comparing(Attendance::getStampTime));

        OffsetDateTime firstIn = null;
        OffsetDateTime lastOut = null;
        OffsetDateTime breakStart = null;
        OffsetDateTime currentIn = null;
        long totalBreakMinutes = 0;
        long netWorkMinutes = 0; // 実労働時間の純粋な合計（休憩時間分を差し引いた時間）

        for (Attendance log : logs) {
            OffsetDateTime stampTime = log.getStampTime();

            if (log.getStampType().equals(workSt)) {
                if (firstIn == null) {
                    firstIn = stampTime;
                }
                currentIn = stampTime; // 新しい勤務セッションの開始

            } else if (log.getStampType().equals(workEd)) {
                lastOut = stampTime;

                // 勤務セッションの終了: 最後の IN から OUT までの時間を実労働時間に加算
                if (currentIn != null) {
                    netWorkMinutes += ChronoUnit.MINUTES.between(currentIn, stampTime);
                    currentIn = null; // 勤務セッション終了
                }

                breakStart = null;

            } else if (log.getStampType().equals(breakSt)) {
                // 休憩開始
                if (currentIn != null && breakStart == null) {
                    // 休憩開始までの時間を実労働時間に加算
                    netWorkMinutes += ChronoUnit.MINUTES.between(currentIn, stampTime);
                    breakStart = stampTime; // 休憩開始時刻を記録
                    currentIn = null; // 勤務セッション中断
                }

            } else if (log.getStampType().equals(breakEd)) {
                // 休憩終了
                if (breakStart != null) {
                    // 休憩時間を計算し、総休憩時間に加算
                    totalBreakMinutes += ChronoUnit.MINUTES.between(breakStart, stampTime);

                    // 休憩終了時刻を新しい勤務セッションの開始点とする
                    currentIn = stampTime;
                    breakStart = null;
                }
            }
        }
        summary.setTotalBreakMinutes((int) totalBreakMinutes);

        if (firstIn != null && lastOut != null) {
            // 時刻抽出: JST ZoneIdを使って LocalTime を抽出
            LocalTime actualInTime = firstIn.atZoneSameInstant(JST_ZONE).toLocalTime();
            LocalTime actualOutTime = lastOut.atZoneSameInstant(JST_ZONE).toLocalTime();

            summary.setActualInTime(actualInTime);
            summary.setActualOutTime(actualOutTime);

            summary.setTotalWorkMinutes((int) Math.max(0, netWorkMinutes));

            summary.setOvertimeMinutes(0);
            summary.setNightShiftMinutes(0);
        } else {
            summary.setTotalWorkMinutes(0);
        }

        return summary;
    }

    /**
     * 最新の勤怠情報を取得するメソッドです.
     *
     * 【機能】
     * 現在の「勤務日」（9:00締めを考慮）における最新の打刻記録をAttendanceテーブルから取得し、DTOとして返します。
     *
     * 【注意事項】
     * 取得範囲は、現在の勤務日の開始時刻（9:00 JST）から現在時刻までです。
     *
     * @param employeeId 従業員ID
     * @return 最新勤怠DTO（Optional）
     */
    public Optional<AttendanceDto> getLatestAttendance(String employeeId) {
        OffsetDateTime now = OffsetDateTime.now(JST_ZONE);
        // 現在時刻から「勤務日」を計算する
        LocalDate currentWorkDate = getWorkDate(now);

        // 取得範囲を現在の勤務日の開始時刻（9:00）から現在時刻までとする
        OffsetDateTime startOfWorkDay = currentWorkDate.atTime(WORK_DAY_CLOSE_TIME).atZone(JST_ZONE).toOffsetDateTime();

        return attendanceRepository
                // 勤務日の開始時刻から now までの最新の打刻を取得
                .findTopByEmployeeEmployeeIdAndStampTimeBetweenOrderByStampTimeDesc(employeeId, startOfWorkDay, now)
                .map(this::convertToDto);
    }

    private AttendanceDto convertToDto(Attendance attendance) {
        AttendanceDto dto = new AttendanceDto();
        dto.setEmployeeId(attendance.getEmployee().getEmployeeId());
        dto.setStampTime(attendance.getStampTime());
        dto.setStampType(attendance.getStampType());
        dto.setNote(null);
        return dto;
    }

    /**
     * 勤怠エンティティをDTOに変換するメソッドです.
     *
     * 【機能】
     * Attendanceエンティティの主要なフィールド（従業員ID、打刻時刻、打刻種別、備考）をAttendanceDtoにマッピングします。
     *
     * 【注意事項】
     * 特になし。
     *
     * @param attendance 勤怠エンティティ
     * @return 勤怠DTO
     */
    public List<AggregatedAttendanceSummaryDto> getAggregatedAttendanceSummary(LocalDate startDate, LocalDate endDate) {
        List<Employee> employees = employeeRepository.findAll();
        List<AggregatedAttendanceSummaryDto> summariesDto = new ArrayList<>();

        List<DailyAttendanceSummary> allSummaries = summaryRepository.findByWorkDateBetweenAndStatusIn(
                startDate,
                endDate,
                List.of(STATUS_FINALIZED, STATUS_APPROVED));

        Map<String, List<DailyAttendanceSummary>> summariesByEmployee = allSummaries.stream()
                .collect(Collectors.groupingBy(DailyAttendanceSummary::getEmployeeId));

        for (Employee employee : employees) {
            String employeeId = employee.getEmployeeId();
            List<DailyAttendanceSummary> employeeSummaries = summariesByEmployee.getOrDefault(employeeId,
                    new ArrayList<>());

            long totalNetWorkMinutes = employeeSummaries.stream().mapToLong(DailyAttendanceSummary::getTotalWorkMinutes)
                    .sum();
            long totalOvertimeMinutes = employeeSummaries.stream().mapToLong(DailyAttendanceSummary::getOvertimeMinutes)
                    .sum();
            long totalLateNightMinutes = employeeSummaries.stream()
                    .mapToLong(DailyAttendanceSummary::getNightShiftMinutes).sum();

            double totalHours = totalNetWorkMinutes / MINUTES_IN_HOUR;
            double overtimeHours = totalOvertimeMinutes / MINUTES_IN_HOUR;
            double lateNightHours = totalLateNightMinutes / MINUTES_IN_HOUR;

            double roundedTotalHours = Math.round(totalHours * HOURS_ROUNDING_SCALE) / HOURS_ROUNDING_SCALE;
            double roundedOvertimeHours = Math.round(overtimeHours * HOURS_ROUNDING_SCALE) / HOURS_ROUNDING_SCALE;
            double roundedLateNightHours = Math.round(lateNightHours * HOURS_ROUNDING_SCALE) / HOURS_ROUNDING_SCALE;

            summariesDto.add(new AggregatedAttendanceSummaryDto(
                    employeeId,
                    employee.getName(),
                    roundedTotalHours,
                    roundedOvertimeHours,
                    roundedLateNightHours));
        }

        return summariesDto;
    }
}