package com.example.attendance.attendance_app.repository;

import com.example.attendance.attendance_app.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

        // 既存のメソッド（打刻ボタン判定用）
        Optional<Attendance> findTopByEmployeeEmployeeIdAndStampTimeBetweenOrderByStampTimeDesc(String employeeId,
                        OffsetDateTime start, OffsetDateTime end);

        // 既存のメソッド（全ログ検索用）
        List<Attendance> findByEmployeeEmployeeId(String employeeId);

        // クエリに employee_id によるフィルタリングを追加
        @Query("SELECT a FROM Attendance a WHERE a.employee.employeeId = :employeeId AND a.stampTime >= :startDateTime AND a.stampTime < :endDateTime")
        List<Attendance> findByPeriod(
                        @Param("employeeId") String employeeId, // ★ パラメータ追加
                        @Param("startDateTime") OffsetDateTime startDateTime,
                        @Param("endDateTime") OffsetDateTime endDateTime);

        // 【新規追加】全件の勤怠ログを打刻日時の降順（最新順）にソートして取得
        List<Attendance> findAllByOrderByStampTimeDesc();
}