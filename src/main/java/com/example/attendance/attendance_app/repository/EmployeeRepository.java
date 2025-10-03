package com.example.attendance.attendance_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.attendance.attendance_app.model.Employee;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmployeeCode(String employeeCode);
}