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
         * * @param employeeId 従業員ID
         * 
         * @param targetDate 計算対象の日付（例: 期間の開始日）
         * @return 当時有効な時給履歴レコード（Optional）
         */
        @Query("SELECT h FROM EmployeeWageHistory h " +
                        "WHERE h.employee.employeeId = :employeeId " +
                        "  AND h.effectiveStartDate <= :targetDate " +
                        "  AND (h.effectiveEndDate IS NULL OR h.effectiveEndDate >= :targetDate)")
        Optional<EmployeeWageHistory> findApplicableWageByEmployeeIdAndDate(
                        @Param("employeeId") String employeeId,
                        @Param("targetDate") LocalDate targetDate);
}