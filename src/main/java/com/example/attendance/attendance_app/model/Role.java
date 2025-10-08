package com.example.attendance.attendance_app.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.persistence.*;

/**
 * 役割マスタ (role) のエンティティです。
 * システム内のアクセス権限グループを定義します。
 */
@Entity
@Table(name = "role")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    /** 主キー (自動採番) */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long id;

    /** 役割名 (例: システム管理者, マネージャー) */
    @Column(name = "role_name", nullable = false, length = 50, unique = true)
    private String roleName;

    /** 権限レベル (数値。大きいほど権限が強い) */
    @Column(name = "role_level", nullable = false)
    private Integer roleLevel;

    /** 役割コード (システム内部識別子。例: ADMIN, MGR) */
    @Column(name = "role_code", nullable = false, length = 20, unique = true)
    private String roleCode;

    // 注: 権限レベルを比較したり、ロジックでコードを直接参照するために使用されます。

    // EmployeeRoleとの@OneToMany関係は、Role側では通常不要です。
}
