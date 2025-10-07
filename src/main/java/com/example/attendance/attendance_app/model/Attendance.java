package com.example.attendance.attendance_app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ★ 修正箇所: 外部キーの型をStringに対応するEmployeeエンティティに変更
    // DBのカラム名: employee_id, 型: VARCHAR(20)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee; // Employee.java の主キーが String であるため、リレーションは正しく機能します

    @Column(name = "stamp_time", nullable = false)
    private OffsetDateTime stampTime;

    @Column(name = "stamp_type", nullable = false, length = 20)
    private String stampType;

    @Column(name = "note")
    private String note;
}