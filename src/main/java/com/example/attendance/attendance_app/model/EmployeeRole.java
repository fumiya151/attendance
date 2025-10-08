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
    @GeneratedValue(strategy = GenerationType.IDENTITY) // ★ 修正済み: 自動採番戦略
    private Long id;

    // 従業員ID (外部キーではないが、論理的にEmployeeを参照)
    @Column(name = "employee_id", length = 20, nullable = false)
    private String employeeId;

    // 役割ID (外部キー)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(name = "assigned_at")
    private OffsetDateTime assignedAt;

    @PrePersist
    protected void onCreate() {
        if (this.assignedAt == null) {
            this.assignedAt = OffsetDateTime.now();
        }
    }
}
