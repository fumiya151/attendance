package com.example.attendance.attendance_app.service;

import com.example.attendance.attendance_app.dto.PayrollDto;
import com.example.attendance.attendance_app.model.Attendance;
import com.example.attendance.attendance_app.model.Employee;
import com.example.attendance.attendance_app.model.EmployeeWageHistory;
import com.example.attendance.attendance_app.repository.AttendanceRepository;
import com.example.attendance.attendance_app.repository.EmployeeRepository;
import com.example.attendance.attendance_app.repository.EmployeeWageHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 従業員の給与計算ロジックを管理するサービスです。
 * 勤怠ログと時給履歴に基づき、指定期間の実労働時間と給与を計算します。
 *
 * 【機能】
 * 期間指定による全従業員の給与計算、時給履歴の参照、休憩時間の自動差し引きを行います。
 *
 * 【注意事項】
 * 正確な計算のため、AttendanceRepositoryに findByPeriod メソッドの実装が必要です。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollService {

    // コンストラクタインジェクション (Lombokの@RequiredArgsConstructorを使用)
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeWageHistoryRepository wageHistoryRepository;

    // システムが基準とするタイムゾーンオフセット (JST: UTC+9)
    private static final ZoneOffset DEFAULT_TIME_ZONE_OFFSET = ZoneOffset.ofHours(9);

    private static final String WAGE_NOT_FOUND_MSG = "従業員ID: {} の有効な時給が見つかりませんでした。計算対象日: {}";

    private static final String STAMP_TYPE_IN = "出勤"; // JSとControllerの定数に合わせるため変更
    private static final String STAMP_TYPE_OUT = "退勤"; // JSとControllerの定数に合わせるため変更
    private static final String STAMP_TYPE_BREAK_START = "休憩開始"; // JSとControllerの定数に合わせるため変更
    private static final String STAMP_TYPE_BREAK_END = "休憩終了"; // JSとControllerの定数に合わせるため変更

    private static final double HOURS_ROUNDING_SCALE = 100.0;
    private static final double MINUTES_IN_HOUR = 60.0;

    /**
     * 指定期間の給与計算を実行し、結果のDTOリストを返却するメソッドです。
     *
     * 【機能】
     * 1. 従業員ごとに、期間開始日時点で有効な時給を取得します。
     * 2. 期間内の勤怠ログを取得し、日ごと・休憩時間を差し引いた実労働時間を計算します。
     * 3. 計算された実労働時間と時給から給与総額を算出します。
     *
     * 【注意事項】
     * 期間内の勤怠ログは、AttendanceRepositoryの findByPeriod メソッドを通じて取得されます。
     * 時給が設定されていない従業員は計算対象からスキップされます。
     *
     * @param startDate 計算開始日
     * @param endDate     計算終了日
     * @return 計算結果のDTOリスト
     */
    public List<PayrollDto> calculatePayroll(LocalDate startDate, LocalDate endDate) {

        List<Employee> employees = employeeRepository.findAll();
        List<PayrollDto> payrolls = new ArrayList<>();

        // 期間の開始日時と終了日時をタイムゾーンオフセット付きで定義
        OffsetDateTime startDateTime = startDate.atStartOfDay().atOffset(DEFAULT_TIME_ZONE_OFFSET);
        OffsetDateTime endDateTime = endDate.plusDays(1).atStartOfDay().atOffset(DEFAULT_TIME_ZONE_OFFSET);

        // 期間内の勤怠データを全て取得
        List<Attendance> allAttendances = attendanceRepository.findByPeriod(startDateTime, endDateTime);

        // 従業員IDごとに勤怠ログをグループ化
        Map<String, List<Attendance>> logsByEmployee = allAttendances.stream()
                .collect(Collectors.groupingBy(a -> a.getEmployee().getEmployeeId()));

        for (Employee employee : employees) {
            String employeeId = employee.getEmployeeId();

            // 1. 時給履歴テーブルから、期間開始日時点で適用される時給を取得
            BigDecimal hourlyWage = wageHistoryRepository
                    .findApplicableWageByEmployeeIdAndDate(employeeId, startDate)
                    .map(EmployeeWageHistory::getHourlyWage)
                    .orElse(BigDecimal.ZERO);

            if (hourlyWage.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn(WAGE_NOT_FOUND_MSG, employeeId, startDate);
                continue; // 時給が0以下の従業員は計算対象外
            }

            List<Attendance> attendances = logsByEmployee.getOrDefault(employeeId, new ArrayList<>());

            // 2. 労働時間計算ロジックを呼び出し、総実労働時間（時間単位）を取得
            double totalHours = calculateTotalWorkingHours(attendances);

            // 3. 給与を計算
            double calculatedSalary = hourlyWage.doubleValue() * totalHours;

            // DTOに追加
            payrolls.add(new PayrollDto(
                    employeeId,
                    employee.getName(),
                    Math.round(totalHours * HOURS_ROUNDING_SCALE) / HOURS_ROUNDING_SCALE, // 小数点第2位で四捨五入
                    Double.valueOf(Math.round(calculatedSalary)) // 整数に丸め
            ));
        }

        return payrolls;
    }

    /**
     * 期間内の全勤怠ログを日ごとに処理し、総実労働時間を計算するメソッドです。
     *
     * 【機能】
     * 勤怠ログを日付ごとにグループ化し、各日の実労働時間を合算して返します。
     *
     * 【注意事項】
     * タイムゾーンは DEFAULT_TIME_ZONE_OFFSET を基準とします。
     *
     * @param attendances 期間内の全勤怠ログ
     * @return 期間の総実労働時間 (時間単位)
     */
    private double calculateTotalWorkingHours(List<Attendance> attendances) {
        if (attendances.isEmpty()) {
            return 0.0;
        }

        // ログを打刻日 (LocalDate) ごとにグループ化
        Map<LocalDate, List<Attendance>> logsByDate = attendances.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getStampTime().atZoneSameInstant(DEFAULT_TIME_ZONE_OFFSET.normalized()).toLocalDate()));

        double totalNetWorkingHours = 0.0;

        // 日付ごとに労働時間を計算
        for (List<Attendance> dailyLogs : logsByDate.values()) {
            // 打刻時間でソート
            dailyLogs.sort(Comparator.comparing(Attendance::getStampTime));

            totalNetWorkingHours += calculateNetWorkingHoursForSingleDay(dailyLogs);
        }

        return totalNetWorkingHours;
    }

    /**
     * 単日分の勤怠ログから実労働時間を計算するロジックです。
     * 複数回の休憩を正しく差し引き、実労働時間のみを算出します。
     *
     * 【機能】
     * IN/BREAK_START/BREAK_END/OUT の打刻ペアを順に追跡し、
     * 勤務時間から休憩時間を自動で差し引いた純粋な実労働時間を算出します。
     *
     * 【注意事項】
     * ログがIN, BREAK_START, BREAK_END, OUTの順番で適切に出現することを前提としています。
     *
     * @param dailyLogs 単日分の勤怠ログリスト (打刻時間でソート済み)
     * @return 当日の実労働時間 (時間単位)
     */
    private double calculateNetWorkingHoursForSingleDay(List<Attendance> dailyLogs) {
        Duration netWorkDuration = Duration.ZERO;
        OffsetDateTime clockInTime = null;
        OffsetDateTime breakStartTime = null;

        for (Attendance attendance : dailyLogs) {
            String type = attendance.getStampType();
            OffsetDateTime stampTime = attendance.getStampTime();

            if (STAMP_TYPE_IN.equals(type)) {
                // 勤務開始
                clockInTime = stampTime;
                breakStartTime = null;
            } else if (STAMP_TYPE_OUT.equals(type) && clockInTime != null) {
                // 退勤: 最後の勤務区間を労働時間に加算
                netWorkDuration = netWorkDuration.plus(Duration.between(clockInTime, stampTime));

                // 勤務終了（ペアをリセット）
                clockInTime = null;
                breakStartTime = null;
            } else if (STAMP_TYPE_BREAK_START.equals(type) && clockInTime != null && breakStartTime == null) {
                // 休憩開始: 休憩開始までの時間を労働時間に加算し、休憩期間を開始
                netWorkDuration = netWorkDuration.plus(Duration.between(clockInTime, stampTime));
                breakStartTime = stampTime; // 休憩開始時刻を保持
                clockInTime = null; // 勤務区間を一時終了

            } else if (STAMP_TYPE_BREAK_END.equals(type) && breakStartTime != null) {
                // 休憩終了: 休憩区間をスキップし、休憩終了時刻を新しい勤務開始時刻とする
                clockInTime = stampTime; // 次の勤務区間の開始点を設定
                breakStartTime = null;
            }
            // ログの整合性が取れないケースは無視
        }

        // 分を時間に変換して返す
        return netWorkDuration.toMinutes() / MINUTES_IN_HOUR;
    }
}