package com.example.attendance.attendance_app.repository;

import com.example.attendance.attendance_app.model.EmployeeTaxInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeTaxInfoRepository extends JpaRepository<EmployeeTaxInfo, String> {

    /**
     * 指定された従業員IDに基づいて、その従業員の税金関連情報を取得します。
     *
     * 【機能】
     * employeeId をキーとして、対応する EmployeeTaxInfo エンティティを検索します。
     *
     * 【注意事項】
     * EmployeeTaxInfoはemployeeIdを主キーとしているため、常に最大1件のレコードが返されます。
     *
     * @param employeeId 検索対象の従業員ID（主キー）
     * @return 該当する税金情報エンティティ（Optional）
     */
    Optional<EmployeeTaxInfo> findByEmployeeId(String employeeId);
}