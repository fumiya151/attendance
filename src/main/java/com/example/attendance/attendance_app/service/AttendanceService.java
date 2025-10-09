package com.example.attendance.attendance_app.service;

import java.util.Optional;
import java.util.List;
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
import java.util.stream.Collectors;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final DailyAttendanceSummaryRepository summaryRepository;

    private static final ZoneId JST_ZONE = ZoneId.of("Asia/Tokyo");

    private final String workSt = "出勤";
    private final String workEd = "退勤";
    private final String breakSt = "休憩開始";
    private final String breakEd = "休憩終了";

    /**
     * 次に有効な打刻種別リストを返すメソッドです.
     *
     * @param employeeId 従業員ID
     * @return 有効な打刻種別リスト
     */
    public List<String> getNextAvailableStampTypes(String employeeId) {
        Optional<AttendanceDto> latestAttendanceOpt = getLatestAttendance(employeeId);

        if (latestAttendanceOpt.isEmpty()) {
            return List.of(workSt);
        }

        String lastStampType = latestAttendanceOpt.get().getStampType();
        switch (lastStampType) {
            case workSt:
                return List.of(workEd, breakSt);
            case workEd:
                return List.of(workSt);
            case breakSt:
                return List.of(breakEd);
            case breakEd:
                return List.of(workEd, breakSt);
            default:
                return List.of();
        }
    }

    /**
     * 勤怠記録を登録するメソッドです.
     *
     * @param request    勤怠リクエスト
     * @param operatorId 勤怠操作を行った従業員ID (打刻処理では通常、request.getEmployeeId()と同じ)
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
        attendance.setStampType(request.getAttendanceType());

        Attendance savedAttendance = attendanceRepository.save(attendance);

        if (request.getAttendanceType().equals(workEd)) {
            LocalDate workDate = stampTime.toLocalDate();
            processCheckoutSummary(request.getEmployeeId(), workDate, operatorId);
        }

        return savedAttendance;
    }

    /**
     * 退勤時の日次集計処理とdaily_attendance_summaryへの登録/更新（UPSERT）を行います.
     *
     * @param employeeId 従業員ID
     * @param workDate     勤務日
     * @param operatorId 集計を更新した従業員ID
     */
    private void processCheckoutSummary(String employeeId, LocalDate workDate, String operatorId) {
        OffsetDateTime startOfDay = workDate.atStartOfDay(JST_ZONE).toOffsetDateTime();
        OffsetDateTime endOfDay = workDate.plusDays(1).atStartOfDay(JST_ZONE).toOffsetDateTime();

        // DB側でemployeeIdによるフィルタリングを行う
        List<Attendance> logs = attendanceRepository.findByPeriod(employeeId, startOfDay, endOfDay);

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
        summaryToSave.setOvertimeMinutes(0);
        summaryToSave.setNightShiftMinutes(0);
        summaryToSave.setCalculatedAt(OffsetDateTime.now(JST_ZONE));

        summaryToSave.setUpdatedById(operatorId);

        summaryRepository.save(summaryToSave);
    }

    /**
     * 打刻ログリストから日次集計オブジェクトを計算・生成します.
     *
     * @param employeeId 従業員ID
     * @param workDate     勤務日
     * @param logs             その日の打刻ログ
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

        // --- サマリーへのセット ---

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
     * @param employeeId 従業員ID
     * @return 最新勤怠DTO（Optional）
     */
    public Optional<AttendanceDto> getLatestAttendance(String employeeId) {
        OffsetDateTime now = OffsetDateTime.now(JST_ZONE);
        OffsetDateTime startOfDay = now.toLocalDate().atStartOfDay(JST_ZONE).toOffsetDateTime();

        return attendanceRepository
                .findTopByEmployeeEmployeeIdAndStampTimeBetweenOrderByStampTimeDesc(employeeId, startOfDay, now)
                .map(this::convertToDto);
    }

    /**
     * 勤怠エンティティをDTOに変換するメソッドです.
     *
     * @param attendance 勤怠エンティティ
     * @return 勤怠DTO
     */
    private AttendanceDto convertToDto(Attendance attendance) {
        AttendanceDto dto = new AttendanceDto();
        dto.setEmployeeId(attendance.getEmployee().getEmployeeId());
        dto.setStampTime(attendance.getStampTime());
        dto.setStampType(attendance.getStampType());
        dto.setNote(attendance.getNote());
        return dto;
    }

    /**
     * 全ての勤怠ログを取得するメソッドです.
     *
     * @return 全勤怠DTOリスト
     */
    public List<AttendanceDto> getAllAttendanceLogs() {
        return attendanceRepository.findAllByOrderByStampTimeDesc().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
}