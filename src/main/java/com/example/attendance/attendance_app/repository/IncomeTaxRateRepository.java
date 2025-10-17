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
     * (甲欄と乙欄の両方が含まれる可能性があります)
     * * @param date 計算対象日
     * 
     * @param taxableIncome 控除後の給与額
     * @return 適用される税額表の行（通常は1行だが、甲欄/乙欄の区別のため2行返る可能性あり）
     */
    @Query("SELECT i FROM IncomeTaxRate i WHERE i.effectiveStartDate <= :date " +
            "AND (i.incomeFrom <= :taxableIncome AND i.incomeTo >= :taxableIncome) " +
            "ORDER BY i.effectiveStartDate DESC")
    List<IncomeTaxRate> findApplicableRates(LocalDate date, BigDecimal taxableIncome);

    // 特定の税区分（甲欄/乙欄）でピンポイントに取得するメソッドも追加可能
    Optional<IncomeTaxRate> findTopByTaxTypeAndEffectiveStartDateLessThanEqualAndIncomeFromLessThanEqualAndIncomeToGreaterThanEqualOrderByEffectiveStartDateDesc(
            String taxType, LocalDate date, BigDecimal incomeFrom, BigDecimal incomeTo);
}