package com.example.attendance.attendance_app.repository;

import com.example.attendance.attendance_app.model.PayrollSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PayrollSettingRepository extends JpaRepository<PayrollSetting, String> {

    /**
     * 指定されたキーと日付に基づいて、現在適用されている設定値を取得します。
     *
     * 【機能】
     * settingKeyが一致し、かつ指定された日付（date）が有効期間内（effectiveStartDate以下 かつ
     * (effectiveEndDateがNULLまたはeffectiveEndDate以上)）にある、最新の設定レコードを検索します。
     *
     * 【注意事項】
     * 有効期間が重複する場合、effectiveStartDateが最も新しいレコード（最新の改定）が返されます。JPQLのLIMIT句を使用しています。
     *
     * @param settingKey 取得する設定のキー
     * @param date       確認日（通常は計算期間の開始日）
     * @return 適用されている設定値を持つ Optional<PayrollSetting>
     */
    @Query("SELECT ps FROM PayrollSetting ps WHERE ps.settingKey = :settingKey " +
            "AND ps.effectiveStartDate <= :date " +
            "AND (ps.effectiveEndDate IS NULL OR ps.effectiveEndDate >= :date) " +
            "ORDER BY ps.effectiveStartDate DESC " +
            "LIMIT 1") // Spring Data JPA 3.x 以降の LIMIT 句に対応
    Optional<PayrollSetting> findApplicableSetting(String settingKey, LocalDate date);
}