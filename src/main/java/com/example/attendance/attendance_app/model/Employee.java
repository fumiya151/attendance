package com.example.attendance.attendance_app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "employee")
public class Employee {

    // 主キー: 従業員コードがVARCHAR主キーとしてemployee_idにリネームされたもの
    @Id
    @Column(name = "employee_id", length = 20)
    private String employeeId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "department", length = 50)
    private String department;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "password", length = 255)
    private String password;
}