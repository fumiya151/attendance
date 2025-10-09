package com.example.attendance.attendance_app.repository;

import com.example.attendance.attendance_app.model.DailyAttendanceSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List; // ★ 追加
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

    // ★ 追加メソッド: 給与計算用 ★

    /**
     * 指定された期間内（workDate BETWEEN startDate AND endDate）の勤怠サマリーを、
     * 指定されたステータス（IN statuses）に含まれるものに限定して検索します。
     * * @param startDate 期間開始日
     * 
     * @param endDate  期間終了日
     * @param statuses 検索対象とするステータス（例: "FINALIZED", "APPROVED"）
     * @return 条件に一致するDailyAttendanceSummaryエンティティのリスト
     */
    List<DailyAttendanceSummary> findByWorkDateBetweenAndStatusIn(LocalDate startDate, LocalDate endDate,
            List<String> statuses);
}