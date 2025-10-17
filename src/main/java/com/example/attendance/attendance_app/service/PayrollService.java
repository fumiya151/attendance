package com.example.attendance.attendance_app.service;

import com.example.attendance.attendance_app.dto.PayrollDto;
import com.example.attendance.attendance_app.model.Employee;
import com.example.attendance.attendance_app.model.EmployeeWageHistory;
import com.example.attendance.attendance_app.model.DailyAttendanceSummary;
import com.example.attendance.attendance_app.model.PayrollSetting;
import com.example.attendance.attendance_app.model.EmployeeTaxInfo;
import com.example.attendance.attendance_app.model.IncomeTaxRate;
import com.example.attendance.attendance_app.repository.EmployeeRepository;
import com.example.attendance.attendance_app.repository.EmployeeWageHistoryRepository;
import com.example.attendance.attendance_app.repository.DailyAttendanceSummaryRepository;
import com.example.attendance.attendance_app.repository.PayrollSettingRepository;
import com.example.attendance.attendance_app.repository.EmployeeTaxInfoRepository;
import com.example.attendance.attendance_app.repository.IncomeTaxRateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final EmployeeTaxInfoRepository employeeTaxInfoRepository;
    private final IncomeTaxRateRepository incomeTaxRateRepository; // ★追加: 税額表リポジトリ★

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

    // ★修正: 簡易税率ではなく、税額表を参照するため削除
    // private static final BigDecimal SIMPLE_INCOME_TAX_RATE =
    // BigDecimal.valueOf(0.05);

    /**
     * 指定期間の給与計算を実行し、結果のDTOリストを返却するメソッドです。
     * ...
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

            // 1. 従業員別控除情報を取得
            EmployeeTaxInfo taxInfo = employeeTaxInfoRepository.findByEmployeeId(employeeId)
                    .orElseGet(() -> {
                        log.warn("従業員ID: {} の税金情報が見つかりませんでした。デフォルト値 (扶養0, 住民税0) を使用します。",
                                employeeId);
                        return new EmployeeTaxInfo(); // デフォルト値が設定された新しいTaxInfoを返す
                    });

            // 2. 時給履歴テーブルから、期間開始日時点で適用される時給を取得 (BigDecimalを使用)
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

            // 3. 労働時間（分）を集計と丸め込み (変更なし)
            long totalNetWorkMinutes = summaries.stream()
                    .mapToLong(DailyAttendanceSummary::getTotalWorkMinutes).sum();
            long totalOvertimeMinutes = summaries.stream()
                    .mapToLong(DailyAttendanceSummary::getOvertimeMinutes).sum();
            long totalLateNightMinutes = summaries.stream()
                    .mapToLong(DailyAttendanceSummary::getNightShiftMinutes).sum();

            double totalHours = totalNetWorkMinutes / MINUTES_IN_HOUR;
            double overtimeHours = totalOvertimeMinutes / MINUTES_IN_HOUR;
            double lateNightHours = totalLateNightMinutes / MINUTES_IN_HOUR;

            double roundedTotalHours = roundHours(totalHours);
            double roundedOvertimeHours = roundHours(overtimeHours);
            double roundedLateNightHours = roundHours(lateNightHours);

            // 4. 支給額の計算 (基本給、各種手当)

            BigDecimal basePaySalaryBd = hourlyWage.multiply(BigDecimal.valueOf(roundedTotalHours));

            BigDecimal overtimePayRate = overtimeRate.subtract(BigDecimal.ONE);
            BigDecimal overtimePayBd = BigDecimal.valueOf(roundedOvertimeHours)
                    .multiply(hourlyWage)
                    .multiply(overtimePayRate)
                    .setScale(0, RoundingMode.HALF_UP);

            BigDecimal lateNightPayRate = lateNightRate.subtract(BigDecimal.ONE);
            BigDecimal lateNightPayBd = BigDecimal.valueOf(roundedLateNightHours)
                    .multiply(hourlyWage)
                    .multiply(lateNightPayRate)
                    .setScale(0, RoundingMode.HALF_UP);

            BigDecimal totalGrossPayBd = basePaySalaryBd
                    .add(overtimePayBd)
                    .add(lateNightPayBd);

            // 5. 控除額の計算

            // 5-1. 社会保険料の計算
            BigDecimal healthFeeBd = totalGrossPayBd.multiply(healthRate).setScale(0, RoundingMode.DOWN);
            BigDecimal pensionFeeBd = totalGrossPayBd.multiply(pensionRate).setScale(0, RoundingMode.DOWN);
            BigDecimal employmentFeeBd = totalGrossPayBd.multiply(employmentRate).setScale(0,
                    RoundingMode.DOWN);

            BigDecimal totalSocialInsuranceFeeBd = healthFeeBd
                    .add(pensionFeeBd)
                    .add(employmentFeeBd);

            // 5-2. 所得税の計算 (★修正: 税額表の参照に切り替え★)

            // 課税対象額 (総支給額 - 社会保険料控除額)
            BigDecimal taxableIncomeBd = totalGrossPayBd.subtract(totalSocialInsuranceFeeBd);

            if (taxableIncomeBd.compareTo(BigDecimal.ZERO) < 0) {
                taxableIncomeBd = BigDecimal.ZERO;
            }

            // ★新規ロジック: 税額表を参照して所得税を決定 (甲欄として計算)★
            BigDecimal incomeTaxBd = calculateIncomeTax(startDate, taxableIncomeBd,
                    taxInfo.getDependentCount());

            // 5-3. 住民税の計算
            // 住民税: DBから取得した月額をそのまま使用
            BigDecimal residentTaxBd = taxInfo.getMonthlyResidentTax().setScale(0, RoundingMode.HALF_UP);

            // 5-4. 控除合計額 (社会保険 + 税金)
            BigDecimal totalDeductionBd = totalSocialInsuranceFeeBd
                    .add(incomeTaxBd)
                    .add(residentTaxBd);

            // 6. 差引支給額
            BigDecimal netPayBd = totalGrossPayBd.subtract(totalDeductionBd);

            // DTOに格納 (BigDecimalからDoubleに変換して格納)
            PayrollDto dto = new PayrollDto(
                    employeeId,
                    employee.getName(),
                    roundedTotalHours,
                    roundedOvertimeHours,
                    roundedLateNightHours,
                    basePaySalaryBd.doubleValue() // basePaySalary
            );

            // 拡張項目を設定
            dto.setOvertimePay(overtimePayBd.doubleValue());
            dto.setLateNightPay(lateNightPayBd.doubleValue());
            dto.setTotalGrossPay(totalGrossPayBd.doubleValue());

            // 社会保険
            dto.setHealthInsuranceFee(healthFeeBd.doubleValue());
            dto.setPensionFee(pensionFeeBd.doubleValue());
            dto.setEmploymentInsuranceFee(employmentFeeBd.doubleValue());

            // 税金
            dto.setIncomeTax(incomeTaxBd.doubleValue()); // 所得税
            dto.setResidentTax(residentTaxBd.doubleValue()); // 住民税

            // 合計と手取り
            dto.setTotalDeduction(totalDeductionBd.doubleValue());
            dto.setNetPay(netPayBd.doubleValue());

            payrolls.add(dto);
        }

        return payrolls;
    }

    // ----------------------------------------------------
    // ★新規追加: 所得税計算ヘルパーメソッド★
    // ----------------------------------------------------
    /**
     * 所得税額表 (甲欄) を参照し、源泉徴収税額を決定します。
     * 課税対象額と扶養人数が完全に一致するレコードがない場合、税額は0とします。
     */
    private BigDecimal calculateIncomeTax(LocalDate date, BigDecimal taxableIncome, int dependentCount) {

        // 課税対象額がゼロ以下の場合は税額ゼロ
        if (taxableIncome.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // 適用される税額表の行をDBから検索
        // ここでは、給与が incomeFrom <= taxableIncome <= incomeTo の範囲にあるものを検索します。
        List<IncomeTaxRate> applicableRates = incomeTaxRateRepository.findApplicableRates(date, taxableIncome);

        // 簡易化のため、「甲欄 (KOU)」のみを対象とします。
        Optional<IncomeTaxRate> taxRateOpt = applicableRates.stream()
                .filter(rate -> "KOU".equals(rate.getTaxType()))
                .findFirst();

        if (taxRateOpt.isEmpty()) {
            log.warn("日付 {}、課税所得 {} に適用される所得税率 (甲欄) が見つかりませんでした。税額0とします。", date, taxableIncome);
            return BigDecimal.ZERO;
        }

        IncomeTaxRate rate = taxRateOpt.get();

        // 扶養人数に基づいて税額を取得
        return switch (dependentCount) {
            case 0 -> rate.getTax0();
            case 1 -> rate.getTax1();
            case 2 -> rate.getTax2();
            // 扶養人数が3人以上の場合、tax_2の税額を適用する (簡易的なフォールバック)
            default -> rate.getTax2();
        };
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