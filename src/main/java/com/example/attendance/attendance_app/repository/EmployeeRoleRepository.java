package com.example.attendance.attendance_app.repository;

import com.example.attendance.attendance_app.model.EmployeeRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRoleRepository extends JpaRepository<EmployeeRole, Long> {
    /**
     * 従業員IDに基づいて、割り当てられた役割情報を全て取得する
     *
     * @param employeeId 従業員ID
     * @return 役割割当エンティティのリスト
     */
    List<EmployeeRole> findByEmployeeId(String employeeId);
}
