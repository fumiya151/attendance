package com.example.attendance.attendance_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.attendance.attendance_app.model.Employee;

import java.util.Optional;
import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmployeeCode(String employeeCode);

    // 氏名またはコードで部分一致検索
    List<Employee> findByNameContainingIgnoreCaseOrEmployeeCodeContainingIgnoreCase(String name, String code);
}