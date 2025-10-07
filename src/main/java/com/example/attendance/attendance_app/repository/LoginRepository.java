package com.example.attendance.attendance_app.repository;

import com.example.attendance.attendance_app.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LoginRepository extends JpaRepository<Employee, String> {
    Optional<Employee> findByEmployeeId(String employeeCode);
}
