package com.example.attendance.attendance_app.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 主キー

    @Column(name = "employee_code", unique = true, nullable = false)
    private String employeeCode; // 従業員コード (ユニーク必須)

    @Column(name = "name", nullable = false)
    private String name; // 氏名

    @Column(name = "email", unique = true, nullable = false, length = 150)
    private String email; // メールアドレス (ユニーク必須)

    @Column(name = "department")
    private String department; // 部署名

    @Column(name = "hire_date")
    private LocalDate hireDate; // 入社日

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true; // 在職状況 (デフォルトは在職中)
    
    // パスワードは後でSpring Securityで暗号化して保存します
    @Column(name = "password") 
    private String password;

    @Column(name = "hourly_wage")
    private Integer hourlyWage; // 時給
}