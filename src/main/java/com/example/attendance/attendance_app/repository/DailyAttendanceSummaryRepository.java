package com.example.attendance.attendance_app.repository;

import com.example.attendance.attendance_app.model.DailyAttendanceSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyAttendanceSummaryRepository extends JpaRepository<DailyAttendanceSummary, Long> {

    /**
     * ID降順で最新の10件の勤怠サマリーを取得します。
     * Spring Data JPAの findTopNBy...OrderBy... 命名規則を利用。
     */
    List<DailyAttendanceSummary> findTop10ByOrderByIdDesc(); // ★★★ 追加: 最新10件を取得するメソッド ★★★

    /**
     * 指定された従業員IDと日付の勤怠集計レコードを検索します。
     * UPSERT処理の際に、既存のレコードをチェックするために使用されます。
     * 
     * @param employeeId 従業員ID
     * @param workDate   勤務日
     * @return DailyAttendanceSummaryエンティティ（Optional）
     */
    Optional<DailyAttendanceSummary> findByEmployeeIdAndWorkDate(String employeeId, LocalDate workDate);

    /**
     * 指定された期間内（workDate BETWEEN startDate AND endDate）の勤怠サマリーを、
     * 指定されたステータス（IN statuses）に含まれるものに限定して検索します。
     * 
     * @param startDate 期間開始日
     * @param endDate    期間終了日
     * @param statuses  検索対象とするステータス（例: "FINALIZED", "APPROVED"）
     * @return 条件に一致するDailyAttendanceSummaryエンティティのリスト
     */
    List<DailyAttendanceSummary> findByWorkDateBetweenAndStatusIn(LocalDate startDate, LocalDate endDate,
            List<String> statuses);

    /**
     * 指定された期間内の勤怠サマリーをすべて取得します。
     * 
     * @param startDate 期間開始日
     * @param endDate   期間終了日
     * @return 条件に一致するDailyAttendanceSummaryエンティティのリスト
     */
    List<DailyAttendanceSummary> findByWorkDateBetween(LocalDate startDate, LocalDate endDate);

    List<DailyAttendanceSummary> findByEmployeeIdAndWorkDateBetween(String employeeId, LocalDate startDate, LocalDate endDate);
}