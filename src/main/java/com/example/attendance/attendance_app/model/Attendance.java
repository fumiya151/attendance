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
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "stamp_time", nullable = false)
    private OffsetDateTime stampTime;

    @Column(name = "stamp_type", nullable = false, length = 20)
    private String stampType;

    @Column(name = "note")
    private String note;
}
