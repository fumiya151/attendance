package com.example.attendance.attendance_app.repository;

import com.example.attendance.attendance_app.model.EmployeeTaxInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeTaxInfoRepository extends JpaRepository<EmployeeTaxInfo, String> {

    /**
     * 指定された従業員の最新の税金関連情報を取得します。
     * (employee_idは主キーなので、そのままfindが利用できますが、ここでは明示的に定義します)
     */
    Optional<EmployeeTaxInfo> findByEmployeeId(String employeeId);
}