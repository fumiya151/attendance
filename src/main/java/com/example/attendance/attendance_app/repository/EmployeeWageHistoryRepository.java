package com.example.attendance.attendance_app.repository;

import com.example.attendance.attendance_app.model.EmployeeWageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface EmployeeWageHistoryRepository extends JpaRepository<EmployeeWageHistory, Long> {

        /**
         * 指定された従業員IDと日付に基づいて、当時有効だった時給のレコードを検索します。
         *
         * 【機能】
         * 従業員IDと targetDate に基づき、その日付が有効期間内（effectiveStartDate <= targetDate かつ
         * (effectiveEndDate is NULL または effectiveEndDate >= targetDate)）にある時給履歴を検索します。
         *
         * 【注意事項】
         * 有効期間が重複するデータが存在する場合、effectiveStartDate が最新のレコード（ORDER BY ... DESC LIMIT
         * 1）を返します。
         *
         * @param employeeId 従業員ID
         * @param targetDate 計算対象の日付（例: 期間の開始日）
         * @return 当時有効な時給履歴レコード（Optional）
         */
        @Query("SELECT h FROM EmployeeWageHistory h " +
                        "WHERE h.employee.employeeId = :employeeId " +
                        " AND h.effectiveStartDate <= :targetDate " + // 開始日がターゲット日以前である
                        " AND (h.effectiveEndDate IS NULL OR h.effectiveEndDate >= :targetDate) " + // 終了日がNULLか、ターゲット日以後である
                        "ORDER BY h.effectiveStartDate DESC " + // 複数の有効な時給がある場合、開始日が最新のものを優先
                        "LIMIT 1") // 1件だけ取得
        Optional<EmployeeWageHistory> findApplicableWageByEmployeeIdAndDate(
                        @Param("employeeId") String employeeId,
                        @Param("targetDate") LocalDate targetDate);
}