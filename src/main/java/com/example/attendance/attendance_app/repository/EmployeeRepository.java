package com.example.attendance.attendance_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.attendance.attendance_app.model.Employee;
import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    // 氏名またはコードで部分一致検索
    List<Employee> findByNameContainingIgnoreCaseOrEmployeeCodeContainingIgnoreCase(String name, String code);
}