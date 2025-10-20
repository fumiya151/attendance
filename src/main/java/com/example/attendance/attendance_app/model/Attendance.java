package com.example.attendance.attendance_app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * 従業員の個々の打刻（タイムスタンプ）の生ログを保持するエンティティです。
 *
 * 【用途】
 * 従業員が打刻機やアプリケーションで行ったIN/OUT/休憩開始/休憩終了といった全ての操作が、
 * このテーブルに記録されます。日次サマリー集計の元データとして利用されます。
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "attendance")
public class Attendance {

    /**
     * 主キー（自動生成）。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 打刻を行った従業員エンティティへの参照。
     * DBのカラム名: employee_id, 型: VARCHAR(20)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee; // Employee.java の主キーが String であるため、リレーションは正しく機能します

    /**
     * 打刻が記録された日時（タイムゾーン情報付き）。
     */
    @Column(name = "stamp_time", nullable = false)
    private OffsetDateTime stampTime;

    /**
     * 打刻の種別（例: IN, OUT, BREAK_START, BREAK_END）。
     */
    @Column(name = "stamp_type", nullable = false, length = 20)
    private String stampType;

    /**
     * 備考またはメモ。
     */
    @Column(name = "note")
    private String note;
}