package com.example.attendance.attendance_app.service;

import com.example.attendance.attendance_app.dto.PayrollDto;
import com.example.attendance.attendance_app.model.Employee;
import com.example.attendance.attendance_app.model.EmployeeWageHistory;
import com.example.attendance.attendance_app.model.DailyAttendanceSummary;
import com.example.attendance.attendance_app.repository.EmployeeRepository;
import com.example.attendance.attendance_app.repository.EmployeeWageHistoryRepository;
import com.example.attendance.attendance_app.repository.DailyAttendanceSummaryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollService {

        // コンストラクタインジェクション
        private final EmployeeRepository employeeRepository;
        private final EmployeeWageHistoryRepository wageHistoryRepository;
        private final DailyAttendanceSummaryRepository summaryRepository;

        private static final double HOURS_ROUNDING_SCALE = 100.0;
        private static final double MINUTES_IN_HOUR = 60.0;
        private static final String WAGE_NOT_FOUND_MSG = "従業員ID: {} の有効な時給が見つかりませんでした。計算対象日: {}";
        // 給与計算対象とする確定ステータス
        private static final String STATUS_FINALIZED = "FINALIZED";
        private static final String STATUS_APPROVED = "APPROVED";

        /**
         * 指定期間の給与計算を実行し、結果のDTOリストを返却するメソッドです。
         *
         * 【機能】
         * 期間内の**確定/承認済み勤怠データ**に基づき、従業員ごとの総労働時間、残業時間、深夜時間を集計し、
         * 適用される時給を用いて給与総額を計算します。
         *
         * 【注意事項】
         * 有効な時給が設定されていない従業員については計算をスキップし、ログに警告を出力します。
         *
         * @param startDate 計算開始日 (期間のinclusive start date)
         * @param endDate   計算終了日 (期間のinclusive end date)
         * @return 計算結果のDTOリスト (List<PayrollDto>)
         */
        public List<PayrollDto> calculatePayroll(LocalDate startDate, LocalDate endDate) {

                List<Employee> employees = employeeRepository.findAll();
                List<PayrollDto> payrolls = new ArrayList<>();

                // 期間内の確定済みサマリーデータを全て取得 (FINALIZED または APPROVED のみ)
                List<DailyAttendanceSummary> allSummaries = summaryRepository.findByWorkDateBetweenAndStatusIn(
                                startDate,
                                endDate,
                                List.of(STATUS_FINALIZED, STATUS_APPROVED));

                // 従業員IDごとにサマリーログをグループ化
                Map<String, List<DailyAttendanceSummary>> summariesByEmployee = allSummaries.stream()
                                .collect(Collectors.groupingBy(DailyAttendanceSummary::getEmployeeId));

                for (Employee employee : employees) {
                        String employeeId = employee.getEmployeeId();

                        // 1. 時給履歴テーブルから、期間開始日時点で適用される時給を取得
                        BigDecimal hourlyWage = wageHistoryRepository
                                        .findApplicableWageByEmployeeIdAndDate(employeeId, startDate)
                                        .map(EmployeeWageHistory::getHourlyWage)
                                        .orElse(BigDecimal.ZERO);

                        if (hourlyWage.compareTo(BigDecimal.ZERO) <= 0) {
                                log.warn(WAGE_NOT_FOUND_MSG, employeeId, startDate);
                                continue;
                        }

                        List<DailyAttendanceSummary> summaries = summariesByEmployee.getOrDefault(employeeId,
                                        new ArrayList<>());

                        // 2. 総実労働時間（分）を計算（サマリーテーブルから合算）
                        long totalNetWorkMinutes = summaries.stream()
                                        // total_work_minutes には休憩時間が引かれた純粋な労働時間が格納されている前提
                                        .mapToLong(DailyAttendanceSummary::getTotalWorkMinutes)
                                        .sum();

                        long totalOvertimeMinutes = summaries.stream()
                                        .mapToLong(DailyAttendanceSummary::getOvertimeMinutes)
                                        .sum();

                        long totalLateNightMinutes = summaries.stream()
                                        .mapToLong(DailyAttendanceSummary::getNightShiftMinutes)
                                        .sum();

                        // 時間単位に変換
                        double totalHours = totalNetWorkMinutes / MINUTES_IN_HOUR;
                        double overtimeHours = totalOvertimeMinutes / MINUTES_IN_HOUR;
                        double lateNightHours = totalLateNightMinutes / MINUTES_IN_HOUR;

                        // 3. 給与を計算
                        double calculatedSalary = hourlyWage.doubleValue() * totalHours;

                        // 小数点第2位で四捨五入（最終労働時間）
                        double roundedTotalHours = Math.round(totalHours * HOURS_ROUNDING_SCALE) / HOURS_ROUNDING_SCALE;
                        double roundedOvertimeHours = Math.round(overtimeHours * HOURS_ROUNDING_SCALE)
                                        / HOURS_ROUNDING_SCALE;
                        double roundedLateNightHours = Math.round(lateNightHours * HOURS_ROUNDING_SCALE)
                                        / HOURS_ROUNDING_SCALE;

                        // DTOに追加
                        payrolls.add(new PayrollDto(
                                        employeeId,
                                        employee.getName(),
                                        roundedTotalHours,
                                        roundedOvertimeHours,
                                        roundedLateNightHours,
                                        Double.valueOf(Math.round(calculatedSalary))));
                }

                return payrolls;
        }
}