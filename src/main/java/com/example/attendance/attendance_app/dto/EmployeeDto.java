package com.example.attendance.attendance_app.dto;

import lombok.Data;

/**
 * 従業員マスタ情報を保持するDTOです。
 *
 * 【用途】
 * 主に従業員一覧表示、詳細編集、および認証関連のデータ連携に利用されます。
 */
@Data
public class EmployeeDto {
    /**
     * 従業員ID（主キー）。
     */
    private String employeeId;

    /**
     * 氏名。
     */
    private String name;

    /**
     * メールアドレス。
     */
    private String email;

    /**
     * 部署名または役職名。
     */
    private String department;

    /**
     * 従業員の在籍ステータス（true: 在籍中/有効, false: 退職/無効）。
     */
    private boolean active;

    /**
     * 割り当てられている役割のID（ロールID）。
     */
    private Long roleId;

    /**
     * 現在の時給（String形式）。
     */
    private String wage;

    /**
     * 扶養人数（所得税計算用）。
     */
    private Integer dependentCount;

    /**
     * 住民税月額（特別徴収）。
     */
    private Double monthlyResidentTax;
}