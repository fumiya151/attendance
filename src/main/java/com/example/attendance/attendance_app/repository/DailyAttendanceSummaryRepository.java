package com.example.attendance.attendance_app.repository;

import com.example.attendance.attendance_app.model.DailyAttendanceSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyAttendanceSummaryRepository extends JpaRepository<DailyAttendanceSummary, Long> {

    /**
     * 指定された従業員IDと日付の勤怠集計レコードを検索します。
     * UPSERT処理の際に、既存のレコードをチェックするために使用されます。
     * * @param employeeId 従業員ID
     * 
     * @param workDate 勤務日
     * @return DailyAttendanceSummaryエンティティ（Optional）
     */
    Optional<DailyAttendanceSummary> findByEmployeeIdAndWorkDate(String employeeId, LocalDate workDate);
}
