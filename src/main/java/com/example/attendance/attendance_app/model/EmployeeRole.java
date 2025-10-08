package com.example.attendance.attendance_app.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.OffsetDateTime;

@Entity
@Table(name = "employee_role", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "employee_id", "role_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRole {

    // Primary Key: BIGSERIAL (Database auto-generated)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 自動採番戦略
    private Long id;

    // 従業員ID (割り当て対象)
    @Column(name = "employee_id", length = 20, nullable = false)
    private String employeeId;

    // 役割ID (外部キー)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    // 割り当て日時（作成日時）
    @Column(name = "assigned_at")
    private OffsetDateTime assignedAt;

    // ★ 追加点: 最終更新日時
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // ★ 追加点: 最終更新を行った従業員のID
    @Column(name = "updated_by_employee_id", length = 20)
    private String updatedByEmployeeId;

    @PrePersist // 挿入前
    protected void onCreate() {
        if (this.assignedAt == null) {
            this.assignedAt = OffsetDateTime.now();
        }
        // 作成時にも更新日時を設定
        this.updatedAt = OffsetDateTime.now();
        // NOTE: updatedByEmployeeId (作成者ID) はService層でセットされる必要があります。
    }

    @PreUpdate // 更新前
    protected void onUpdate() {
        // 更新日時を現在時刻に設定
        this.updatedAt = OffsetDateTime.now();
        // NOTE: updatedByEmployeeId (更新者ID) はService層でセットされる必要があります。
    }
}