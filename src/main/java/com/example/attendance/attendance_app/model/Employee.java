package com.example.attendance.attendance_app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 従業員マスタ情報を保持するエンティティです。
 *
 * 【用途】
 * 認証情報、基本属性、在籍状態など、従業員に関する全ての静的データを管理します。
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "employee")
public class Employee {

    /**
     * 主キー: 従業員コード（ログインIDとして使用）。
     */
    @Id
    @Column(name = "employee_id", length = 20)
    private String employeeId;

    /**
     * 氏名。
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * メールアドレス。
     */
    @Column(name = "email", nullable = false, length = 150)
    private String email;

    /**
     * 所属部署または役職。
     */
    @Column(name = "department", length = 50)
    private String department;

    /**
     * 入社年月日。
     */
    @Column(name = "hire_date")
    private LocalDate hireDate;

    /**
     * 在籍ステータス（true: 在籍中/有効, false: 退職/無効）。
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * パスワード（BCryptでハッシュ化された文字列）。
     */
    @Column(name = "password", length = 255)
    private String password;

    /**
     * レコード作成日時。
     */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /**
     * レコード最終更新日時。
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * エンティティが永続化される直前に実行されるコールバックメソッド。
     *
     * 【機能】
     * `createdAt` と `updatedAt` を現在時刻（タイムゾーン情報付き）で初期設定します。
     */
    @PrePersist // 挿入前
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        // 作成時と更新時を同時に設定
        this.createdAt = now;
        this.updatedAt = now;
    }

    /**
     * エンティティが更新される直前に実行されるコールバックメソッド。
     *
     * 【機能】
     * `updatedAt` を現在時刻（タイムゾーン情報付き）に更新します。
     */
    @PreUpdate // 更新前
    protected void onUpdate() {
        // 更新時に更新日時を現在時刻に設定
        this.updatedAt = OffsetDateTime.now();
    }
}