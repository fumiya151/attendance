package com.example.attendance.attendance_app.repository;

import com.example.attendance.attendance_app.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query; // クエリのために必要
import org.springframework.data.repository.query.Param; // クエリのために必要
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    // 既存のメソッド
    Optional<Attendance> findTopByEmployeeEmployeeIdAndStampTimeBetweenOrderByStampTimeDesc(String employeeId,
            OffsetDateTime start, OffsetDateTime end);

    List<Attendance> findByEmployeeEmployeeId(String employeeId);

        /**
         * 【新規追加】指定された期間内の全ての勤怠ログを取得するメソッド。
         * PayrollService のエラーを解消し、給与計算に必要な全データを取得します。
         */
        @Query("SELECT a FROM Attendance a WHERE a.stampTime >= :startDateTime AND a.stampTime < :endDateTime")
        List<Attendance> findByPeriod(
                        @Param("startDateTime") OffsetDateTime startDateTime,
                        @Param("endDateTime") OffsetDateTime endDateTime);
}