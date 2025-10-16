package com.example.attendance.attendance_app.service;

import com.example.attendance.attendance_app.dto.PayrollDto;
import com.example.attendance.attendance_app.model.Employee;
import com.example.attendance.attendance_app.model.EmployeeWageHistory;
import com.example.attendance.attendance_app.model.DailyAttendanceSummary;
import com.example.attendance.attendance_app.model.PayrollSetting;
import com.example.attendance.attendance_app.repository.EmployeeRepository;
import com.example.attendance.attendance_app.repository.EmployeeWageHistoryRepository;
import com.example.attendance.attendance_app.repository.DailyAttendanceSummaryRepository;
import com.example.attendance.attendance_app.repository.PayrollSettingRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollService {

        // コンストラクタインジェクション (既存)
        private final EmployeeRepository employeeRepository;
        private final EmployeeWageHistoryRepository wageHistoryRepository;
        private final DailyAttendanceSummaryRepository summaryRepository;
        private final PayrollSettingRepository payrollSettingRepository;

        private static final double HOURS_ROUNDING_SCALE = 100.0;
        private static final double MINUTES_IN_HOUR = 60.0;
        private static final String WAGE_NOT_FOUND_MSG = "従業員ID: {} の有効な時給が見つかりませんでした。計算対象日: {}";

        // 給与計算対象とする確定ステータス
        private static final String STATUS_FINALIZED = "FINALIZED";
        private static final String STATUS_APPROVED = "APPROVED";

        // 設定キー定数
        private static final String KEY_OVERTIME_RATE = "OVERTIME_RATE";
        private static final String KEY_LATE_NIGHT_RATE = "LATE_NIGHT_RATE";
        private static final String KEY_HEALTH_INSURANCE_RATE_EMP = "HEALTH_INSURANCE_RATE_EMP";
        private static final String KEY_PENSION_RATE_EMP = "PENSION_RATE_EMP";
        private static final String KEY_EMPLOYMENT_INSURANCE_RATE = "EMPLOYMENT_INSURANCE_RATE";

        /**
         * 指定期間の給与計算を実行し、結果のDTOリストを返却するメソッドです。
         *
         * 【機能】
         * 期間内の確定/承認済み勤怠データに基づき、基本給、残業手当、深夜手当、および各種保険料を計算し、
         * 拡張された PayrollDto に格納します。
         *
         * @param startDate 計算開始日 (期間のinclusive start date)
         * @param endDate     計算終了日 (期間のinclusive end date)
         * @return 計算結果のDTOリスト (List<PayrollDto>)
         */
        public List<PayrollDto> calculatePayroll(LocalDate startDate, LocalDate endDate) {

                List<Employee> employees = employeeRepository.findAll();
                List<PayrollDto> payrolls = new ArrayList<>();

                // 期間内の確定済みサマリーデータを全て取得
                List<DailyAttendanceSummary> allSummaries = summaryRepository.findByWorkDateBetweenAndStatusIn(
                                startDate,
                                endDate,
                                List.of(STATUS_FINALIZED, STATUS_APPROVED));

                // 従業員IDごとにサマリーログをグループ化
                Map<String, List<DailyAttendanceSummary>> summariesByEmployee = allSummaries.stream()
                                .collect(Collectors.groupingBy(DailyAttendanceSummary::getEmployeeId));

                // 適用される各種設定値を取得
                BigDecimal overtimeRate = getSettingValue(KEY_OVERTIME_RATE, startDate, 1.25);
                BigDecimal lateNightRate = getSettingValue(KEY_LATE_NIGHT_RATE, startDate, 1.25);
                BigDecimal healthRate = getSettingValue(KEY_HEALTH_INSURANCE_RATE_EMP, startDate, 0.05);
                BigDecimal pensionRate = getSettingValue(KEY_PENSION_RATE_EMP, startDate, 0.0915);
                BigDecimal employmentRate = getSettingValue(KEY_EMPLOYMENT_INSURANCE_RATE, startDate, 0.0055);

                for (Employee employee : employees) {
                        String employeeId = employee.getEmployeeId();

                        // 1. 時給履歴テーブルから、期間開始日時点で適用される時給を取得 (BigDecimalを使用)
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

                        // 2. 労働時間（分）を集計
                        long totalNetWorkMinutes = summaries.stream()
                                        .mapToLong(DailyAttendanceSummary::getTotalWorkMinutes)
                                        .sum();
                        long totalOvertimeMinutes = summaries.stream()
                                        .mapToLong(DailyAttendanceSummary::getOvertimeMinutes)
                                        .sum();
                        long totalLateNightMinutes = summaries.stream()
                                        .mapToLong(DailyAttendanceSummary::getNightShiftMinutes)
                                        .sum();

                        // 時間単位に変換し丸め込み
                        double totalHours = totalNetWorkMinutes / MINUTES_IN_HOUR;
                        double overtimeHours = totalOvertimeMinutes / MINUTES_IN_HOUR;
                        double lateNightHours = totalLateNightMinutes / MINUTES_IN_HOUR;

                        double roundedTotalHours = roundHours(totalHours);
                        double roundedOvertimeHours = roundHours(overtimeHours);
                        double roundedLateNightHours = roundHours(lateNightHours);

                        // 3. 支給額の計算

                        // 3-1. 基本給 (総労働時間 * 基本時給) - これが既存の calculatedSalary に相当
                        BigDecimal basePaySalaryBd = hourlyWage.multiply(BigDecimal.valueOf(roundedTotalHours));

                        // 3-2. 残業手当 (残業時間 * 基本時給 * (割増率 - 1))
                        // ここで計算するのは「基本時給を超過した割増分」のみ
                        BigDecimal overtimePayRate = overtimeRate.subtract(BigDecimal.ONE);
                        BigDecimal overtimePayBd = BigDecimal.valueOf(roundedOvertimeHours)
                                        .multiply(hourlyWage)
                                        .multiply(overtimePayRate)
                                        .setScale(0, RoundingMode.HALF_UP); // 円未満四捨五入

                        // 3-3. 深夜手当 (深夜時間 * 基本時給 * (割増率 - 1))
                        BigDecimal lateNightPayRate = lateNightRate.subtract(BigDecimal.ONE);
                        BigDecimal lateNightPayBd = BigDecimal.valueOf(roundedLateNightHours)
                                        .multiply(hourlyWage)
                                        .multiply(lateNightPayRate)
                                        .setScale(0, RoundingMode.HALF_UP); // 円未満四捨五入

                        // 3-4. 支給合計額 (給与総額)
                        BigDecimal totalGrossPayBd = basePaySalaryBd
                                        .add(overtimePayBd)
                                        .add(lateNightPayBd);

                        // 4. 控除額の計算 (簡易版: 支給総額に料率をかける)
                        // (通常は標準報酬月額を基に計算しますが、ここでは支給総額を代用)

                        // 健康保険料: 支給総額 * 健康保険料率 (端数処理: 50銭以下切り捨て)
                        BigDecimal healthFeeBd = totalGrossPayBd
                                        .multiply(healthRate)
                                        .setScale(0, RoundingMode.DOWN);

                        // 厚生年金保険料: 支給総額 * 厚生年金保険料率 (端数処理: 50銭以下切り捨て)
                        BigDecimal pensionFeeBd = totalGrossPayBd
                                        .multiply(pensionRate)
                                        .setScale(0, RoundingMode.DOWN);

                        // 雇用保険料: 支給総額 * 雇用保険料率 (端数処理: 切り捨て)
                        BigDecimal employmentFeeBd = totalGrossPayBd
                                        .multiply(employmentRate)
                                        .setScale(0, RoundingMode.DOWN);

                        // 控除合計額
                        BigDecimal totalDeductionBd = healthFeeBd
                                        .add(pensionFeeBd)
                                        .add(employmentFeeBd);

                        // 5. 差引支給額
                        BigDecimal netPayBd = totalGrossPayBd.subtract(totalDeductionBd);

                        // DTOに格納 (BigDecimalからDoubleに変換して格納)
                        PayrollDto dto = new PayrollDto(
                                        employeeId,
                                        employee.getName(),
                                        roundedTotalHours,
                                        roundedOvertimeHours,
                                        roundedLateNightHours,
                                        basePaySalaryBd.doubleValue() // basePaySalary (旧 calculatedSalary)
                        );

                        // 拡張項目を設定
                        dto.setOvertimePay(overtimePayBd.doubleValue());
                        dto.setLateNightPay(lateNightPayBd.doubleValue());
                        dto.setTotalGrossPay(totalGrossPayBd.doubleValue());

                        dto.setHealthInsuranceFee(healthFeeBd.doubleValue());
                        dto.setPensionFee(pensionFeeBd.doubleValue());
                        dto.setEmploymentInsuranceFee(employmentFeeBd.doubleValue());
                        dto.setTotalDeduction(totalDeductionBd.doubleValue());
                        dto.setNetPay(netPayBd.doubleValue());

                        payrolls.add(dto);
                }

                return payrolls;
        }

        // ----------------------------------------------------
        // ヘルパーメソッド for 労働時間の丸め込み
        // ----------------------------------------------------
        private double roundHours(double hours) {
                return Math.round(hours * HOURS_ROUNDING_SCALE) / HOURS_ROUNDING_SCALE;
        }

        // ----------------------------------------------------
        // ヘルパーメソッド for 設定値の取得
        // ----------------------------------------------------
        /**
         * PayrollSettingsから指定されたキーの設定値を取得します。見つからない場合はデフォルト値を返します。
         */
        private BigDecimal getSettingValue(String key, LocalDate date, double defaultValue) {
                return payrollSettingRepository.findApplicableSetting(key, date)
                                .map(PayrollSetting::getSettingValue)
                                .orElseGet(() -> {
                                        log.warn("設定キー {} の有効な値が見つかりませんでした。デフォルト値 {} を使用します。", key, defaultValue);
                                        return BigDecimal.valueOf(defaultValue);
                                });
        }
}