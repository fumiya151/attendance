package com.example.attendance.attendance_app.repository;

import com.example.attendance.attendance_app.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findTopByEmployeeIdAndStampTimeBetweenOrderByStampTimeDesc(Long employeeId, OffsetDateTime start, OffsetDateTime end);

    List<Attendance> findByEmployeeId(Long employeeId);
}