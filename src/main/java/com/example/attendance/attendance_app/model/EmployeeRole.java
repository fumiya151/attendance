package com.example.attendance.attendance_app.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.OffsetDateTime;

/**
 * 従業員に割り当てられた役割（ロール）の関連付けを保持するエンティティです。
 *
 * 【用途】
 * どの従業員がどのシステム権限（Role）を持っているかを定義します。
 * employee_id と role_id の組み合わせは一意です（一人の従業員に同じロールを二重に割り当てない）。
 */
@Entity
@Table(name = "employee_role", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "employee_id", "role_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeRole {

    /**
     * 主キー（自動採番）。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 従業員ID（割り当て対象）。
     */
    @Column(name = "employee_id", length = 20, nullable = false)
    private String employeeId;

    /**
     * 役割エンティティへの参照（外部キー）。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    /**
     * 役割が割り当てられた日時（作成日時）。
     */
    @Column(name = "assigned_at")
    private OffsetDateTime assignedAt;

    /**
     * レコード最終更新日時。
     */
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    /**
     * レコード最終更新を行った従業員のID。
     */
    @Column(name = "updated_by_employee_id", length = 20)
    private String updatedByEmployeeId;

    /**
     * エンティティが永続化される直前に実行されるコールバックメソッド。
     *
     * 【機能】
     * `assignedAt` と `updatedAt` を現在時刻（タイムゾーン情報付き）で初期設定します。
     */
    @PrePersist // 挿入前
    protected void onCreate() {
        if (this.assignedAt == null) {
            this.assignedAt = OffsetDateTime.now();
        }
        // 作成時にも更新日時を設定
        this.updatedAt = OffsetDateTime.now();
        // NOTE: updatedByEmployeeId (作成者ID) はService層でセットされる必要があります。
    }

    /**
     * エンティティが更新される直前に実行されるコールバックメソッド。
     *
     * 【機能】
     * `updatedAt` を現在時刻（タイムゾーン情報付き）に更新します。
     */
    @PreUpdate // 更新前
    protected void onUpdate() {
        // 更新日時を現在時刻に設定
        this.updatedAt = OffsetDateTime.now();
        // NOTE: updatedByEmployeeId (更新者ID) はService層でセットされる必要があります。
    }
}