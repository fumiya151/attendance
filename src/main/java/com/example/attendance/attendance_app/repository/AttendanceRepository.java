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

        /**
         * 【打刻ボタン判定用】本日中の最新打刻を1件取得します。
         * 
         * @param employeeId 従業員ID (String)
         */
        Optional<Attendance> findTopByEmployeeEmployeeIdAndStampTimeBetweenOrderByStampTimeDesc(String employeeId,
                        OffsetDateTime start, OffsetDateTime end);

        /**
         * 【全ログ検索用】指定従業員の全期間の勤怠ログを取得します。
         * 
         * @param employeeId 従業員ID (String)
         */
        List<Attendance> findByEmployeeEmployeeId(String employeeId);

        /**
         * 【給与計算用】指定された期間内の全ての勤怠ログを取得するメソッド。
         */
        @Query("SELECT a FROM Attendance a WHERE a.stampTime >= :startDateTime AND a.stampTime < :endDateTime")
        List<Attendance> findByPeriod(
                        @Param("startDateTime") OffsetDateTime startDateTime,
                        @Param("endDateTime") OffsetDateTime endDateTime);
}