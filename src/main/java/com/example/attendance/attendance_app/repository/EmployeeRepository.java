package com.example.attendance.attendance_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.attendance.attendance_app.model.Employee;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {
    
}