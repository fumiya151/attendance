package com.example.attendance.attendance_app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime; // ★ 追加

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

    // --- ★ 追加: 監査フィールド ---

    /**
     * レコード作成日時
     */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /**
     * レコード最終更新日時
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // --- ★ 追加: 自動設定ロジック ---

    @PrePersist // 挿入前
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        // 作成時と更新時を同時に設定
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate // 更新前
    protected void onUpdate() {
        // 更新時に更新日時を現在時刻に設定
        this.updatedAt = OffsetDateTime.now();
    }
}