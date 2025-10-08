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
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    // 【追加】集計テーブル操作用のリポジトリを注入
    private final DailyAttendanceSummaryRepository summaryRepository;

    // JST (UTC+9) タイムゾーンを定義
    private static final ZoneId JST_ZONE = ZoneId.of("Asia/Tokyo");

    // 打刻種別の定数
    private final String workSt = "出勤";
    private final String workEd = "退勤";
    private final String breakSt = "休憩開始";
    private final String breakEd = "休憩終了";

    /**
     * 次に有効な打刻種別リストを返すメソッドです.
     *
     * 【機能】
     * 指定従業員の最新打刻状態から、次に有効な打刻種別（出勤・退勤・休憩開始・休憩終了）を判定しリストで返します。
     *
     * 【注意事項】
     * 該当データがない場合は「出勤」のみ有効となります。
     *
     * @param employeeId 従業員ID
     * @return 有効な打刻種別リスト
     */
    public List<String> getNextAvailableStampTypes(String employeeId) {
        // getLatestAttendance は「本日中の」最新打刻を取得する
        Optional<AttendanceDto> latestAttendanceOpt = getLatestAttendance(employeeId);

        if (latestAttendanceOpt.isEmpty()) {
            // 本日中の打刻がない場合、必ず「出勤」が有効
            return List.of(workSt);
        }

        String lastStampType = latestAttendanceOpt.get().getStampType();
        switch (lastStampType) {
            case workSt: // 出勤
                return List.of(workEd, breakSt);
            case workEd: // 退勤
                // 本日の勤務は終了とみなし、次の出勤を許可する（翌日以降の打刻用）
                return List.of(workSt);
            case breakSt: // 休憩開始
                return List.of(breakEd);
            case breakEd: // 休憩終了
                return List.of(workEd, breakSt);
            default:
                return List.of();
        }
    }

    /**
     * 勤怠記録を登録するメソッドです.
     *
     * 【機能】
     * 勤怠リクエストを受け取り、Attendanceテーブルに生ログを登録します。
     * 打刻種別が「退勤」の場合、同時に日次集計処理（daily_attendance_summaryへの登録/更新）を実行します。
     *
     * 【注意事項】
     * 従業員が見つからない場合はRuntimeExceptionをスローします。
     *
     * @param request 勤怠リクエスト
     * @return 登録された勤怠エンティティ
     */
    @Transactional
    public Attendance recordAttendance(AttendanceRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + request.getEmployeeId()));

        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        // JST (Asia/Tokyo) タイムゾーンで現在時刻を取得し、DBに保存
        OffsetDateTime stampTime = OffsetDateTime.now(JST_ZONE);
        attendance.setStampTime(stampTime);
        attendance.setStampType(request.getAttendanceType());

        Attendance savedAttendance = attendanceRepository.save(attendance);

        // 打刻種別が「退勤」の場合、日次集計処理を実行
        if (request.getAttendanceType().equals(workEd)) {
            LocalDate workDate = stampTime.toLocalDate();
            processCheckoutSummary(request.getEmployeeId(), workDate);
        }

        return savedAttendance;
    }

    /**
     * 退勤時の日次集計処理とdaily_attendance_summaryへの登録/更新（UPSERT）を行います.
     *
     * 【機能】
     * 指定日のAttendanceログを取得し、総労働時間と総休憩時間を計算後、
     * daily_attendance_summaryテーブルに登録または更新（UPSERT）します。
     *
     * 【注意事項】
     * この処理はトランザクション内で実行されることを想定しています。
     *
     * @param employeeId 従業員ID
     * @param workDate   勤務日
     */
    private void processCheckoutSummary(String employeeId, LocalDate workDate) {
        // 1. その日の全打刻ログを取得
        OffsetDateTime startOfDay = workDate.atStartOfDay(JST_ZONE).toOffsetDateTime();
        OffsetDateTime endOfDay = workDate.plusDays(1).atStartOfDay(JST_ZONE).toOffsetDateTime();

        // findByPeriodはTimestampの範囲検索であるため、これを利用して全ログを取得
        List<Attendance> logs = attendanceRepository.findByPeriod(startOfDay, endOfDay).stream()
                .filter(a -> a.getEmployee().getEmployeeId().equals(employeeId))
                .collect(Collectors.toList());

        // 2. 勤務時間の計算
        DailyAttendanceSummary calculatedSummary = calculateDailySummary(employeeId, workDate, logs);

        // 3. daily_attendance_summaryテーブルを検索し、UPSERTを実行
        Optional<DailyAttendanceSummary> existingSummaryOpt = summaryRepository.findByEmployeeIdAndWorkDate(employeeId,
                workDate);

        DailyAttendanceSummary summaryToSave = existingSummaryOpt.orElseGet(DailyAttendanceSummary::new);

        // 新規作成の場合、基本情報をセット
        if (summaryToSave.getWorkDate() == null) {
            summaryToSave.setEmployeeId(employeeId);
            summaryToSave.setWorkDate(workDate);
            summaryToSave.setStatus("PENDING"); // 初期ステータス
        }

        // 計算結果で上書き
        summaryToSave.setActualInTime(calculatedSummary.getActualInTime());
        summaryToSave.setActualOutTime(calculatedSummary.getActualOutTime());
        summaryToSave.setTotalWorkMinutes(calculatedSummary.getTotalWorkMinutes());
        summaryToSave.setTotalBreakMinutes(calculatedSummary.getTotalBreakMinutes());
        summaryToSave.setOvertimeMinutes(0); // 簡略化のため一旦0
        summaryToSave.setNightShiftMinutes(0); // 簡略化のため一旦0
        summaryToSave.setCalculatedAt(OffsetDateTime.now(JST_ZONE));

        // 保存（既存なら更新、新規なら登録）
        summaryRepository.save(summaryToSave);
    }

    /**
     * 打刻ログリストから日次集計オブジェクトを計算・生成します.
     *
     * 【機能】
     * ログを分析し、最初/最後の出退勤時刻、および複数回を含む総休憩時間、さらに複数回の出退勤に対応した
     * 実労働時間を計算してサマリーオブジェクトに格納します。
     *
     * 【注意事項】
     * 残業・深夜計算は含まれていません（簡略化のため）。
     *
     * @param employeeId 従業員ID
     * @param workDate   勤務日
     * @param logs       その日の打刻ログ
     * @return 計算結果が格納されたDailyAttendanceSummaryオブジェクト
     */
    private DailyAttendanceSummary calculateDailySummary(String employeeId, LocalDate workDate, List<Attendance> logs) {
        DailyAttendanceSummary summary = new DailyAttendanceSummary();

        // ログを時系列順にソート
        logs.sort((a, b) -> a.getStampTime().compareTo(b.getStampTime()));

        OffsetDateTime firstIn = null;
        OffsetDateTime lastOut = null;
        OffsetDateTime breakStart = null;
        OffsetDateTime currentIn = null; // 現在のセッションの出勤時刻
        long totalBreakMinutes = 0;
        long totalWorkMinutes = 0; // IN/OUTペアごとの合計労働時間（休憩未控除）

        for (Attendance log : logs) {
            OffsetDateTime stampTime = log.getStampTime();

            if (log.getStampType().equals(workSt)) {
                // 1. 最初の出勤時刻を記録
                if (firstIn == null) {
                    firstIn = stampTime;
                }
                // 2. 現在の勤務セッションの開始時刻を記録
                currentIn = stampTime;

            } else if (log.getStampType().equals(workEd)) {
                // 1. 最後の退勤時刻を記録
                lastOut = stampTime;

                // 2. 勤務セッションの労働時間を計算（複数回の出退勤に対応）
                if (currentIn != null) {
                    totalWorkMinutes += ChronoUnit.MINUTES.between(currentIn, stampTime);
                    currentIn = null; // 勤務セッション終了
                }

            } else if (log.getStampType().equals(breakSt)) {
                // 休憩開始
                if (breakStart == null) {
                    breakStart = stampTime;
                }
            } else if (log.getStampType().equals(breakEd)) {
                // 休憩終了
                if (breakStart != null) {
                    // 休憩時間を計算し、総休憩時間に加算
                    totalBreakMinutes += ChronoUnit.MINUTES.between(breakStart, stampTime);
                    breakStart = null; // 休憩をリセット
                }
            }
        }

        // 休憩時間のセット
        summary.setTotalBreakMinutes((int) totalBreakMinutes);

        if (firstIn != null && lastOut != null) {
            // 出退勤時刻のセット (OffsetDateTimeからLocalTimeに変換)
            summary.setActualInTime(firstIn.atZoneSameInstant(JST_ZONE).toLocalTime());
            summary.setActualOutTime(lastOut.atZoneSameInstant(JST_ZONE).toLocalTime());

            // 最終的な実労働時間 = (IN/OUTペアの合計労働時間) - (休憩時間の合計)
            // 休憩は勤務時間中に取るものとして、IN/OUTペアの合計時間から引く
            long netWorkMinutes = totalWorkMinutes - totalBreakMinutes;

            summary.setTotalWorkMinutes((int) Math.max(0, netWorkMinutes));
        } else {
            // 出退勤が揃っていない場合は0
            summary.setTotalWorkMinutes(0);
        }

        return summary;
    }

    /**
     * 最新の勤怠情報を取得するメソッドです.
     *
     * 【機能】
     * 指定従業員の本日中の最新打刻情報を返却します。
     *
     * 【注意事項】
     * 該当データがない場合は空を返します。
     *
     * @param employeeId 従業員ID
     * @return 最新勤怠DTO（Optional）
     */
    public Optional<AttendanceDto> getLatestAttendance(String employeeId) {
        // JST (Asia/Tokyo) を基準に「今」の時刻を取得
        OffsetDateTime now = OffsetDateTime.now(JST_ZONE);

        // 今日の日付の開始時刻（00:00:00 JST）をOffsetDateTimeとして取得
        OffsetDateTime startOfDay = now.toLocalDate().atStartOfDay(JST_ZONE).toOffsetDateTime();

        // Repository のメソッド名が
        // findTopByEmployeeEmployeeIdAndStampTimeBetweenOrderByStampTimeDesc であることを前提
        return attendanceRepository
                .findTopByEmployeeEmployeeIdAndStampTimeBetweenOrderByStampTimeDesc(employeeId, startOfDay, now)
                .map(this::convertToDto);
    }

    /**
     * 勤怠エンティティをDTOに変換するメソッドです.
     *
     * 【機能】
     * エンティティの各項目をDTOにセットします。
     *
     * 【注意事項】
     * 特になし
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
     * 【機能】
     * データベースに存在する全ての勤怠記録を、打刻日時の新しい順（降順）にソートし、DTOリストとして返却します。
     *
     * @return 全勤怠DTOリスト
     */
    public List<AttendanceDto> getAllAttendanceLogs() {
        return attendanceRepository.findAllByOrderByStampTimeDesc().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
}
