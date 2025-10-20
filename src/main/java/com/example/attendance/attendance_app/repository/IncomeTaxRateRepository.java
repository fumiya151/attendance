package com.example.attendance.attendance_app.repository;

import com.example.attendance.attendance_app.model.IncomeTaxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

@Repository
public interface IncomeTaxRateRepository extends JpaRepository<IncomeTaxRate, Long> {

    /**
     * 指定された日付と社会保険料控除後の給与額に基づいて、適用される税額表の全エントリを取得します。
     *
     * 【機能】
     * 計算対象日以前に適用開始されている税額表のうち、控除後の給与額（taxableIncome）が
     * 所得の範囲（incomeFrom以上 and incomeTo以下）に含まれる全ての行を取得します。
     *
     * 【注意事項】
     * 適用開始日が最新のレコードを優先してソートされます。甲欄と乙欄の両方が含まれる可能性があります。
     *
     * @param date          計算対象日
     * @param taxableIncome 控除後の給与額
     * @return 適用される税額表の行のリスト（通常は1〜2行）
     */
    @Query("SELECT i FROM IncomeTaxRate i WHERE i.effectiveStartDate <= :date " +
            "AND (i.incomeFrom <= :taxableIncome AND i.incomeTo >= :taxableIncome) " +
            "ORDER BY i.effectiveStartDate DESC")
    List<IncomeTaxRate> findApplicableRates(LocalDate date, BigDecimal taxableIncome);

    /**
     * 指定された税区分（甲欄/乙欄）における、最も新しい適用開始日の税額表レコードをピンポイントで取得します。
     *
     * 【機能】
     * 税区分、計算対象日、および給与額の範囲に合致するレコードのうち、effectiveStartDateが最も新しいものを1件取得します。
     *
     * 【注意事項】
     * Spring Data JPAの命名規則を利用し、特定の税区分の最新の有効な税額行を効率的に検索します。
     *
     * @param taxType    税額表の区分（例: KOU, OTSU）
     * @param date       計算対象日
     * @param incomeFrom 控除後の給与額の下限（検索範囲の確認用）
     * @param incomeTo   控除後の給与額の上限（検索範囲の確認用）
     * @return 該当する税額表の単一行（Optional）
     */
    Optional<IncomeTaxRate> findTopByTaxTypeAndEffectiveStartDateLessThanEqualAndIncomeFromLessThanEqualAndIncomeToGreaterThanEqualOrderByEffectiveStartDateDesc(
            String taxType, LocalDate date, BigDecimal incomeFrom, BigDecimal incomeTo);
}