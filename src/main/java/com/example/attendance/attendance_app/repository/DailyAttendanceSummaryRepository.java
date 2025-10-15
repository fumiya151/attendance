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
     *
     * 【機能】
     * データベースに存在する勤怠サマリーのうち、IDが大きい順（＝最新のデータ）から**最大10件**を取得します。
     *
     * 【注意事項】
     * Spring Data JPAの findTopNBy...OrderBy... 命名規則を利用しています。
     *
     * @return 最新10件の DailyAttendanceSummaryエンティティのリスト
     */
    List<DailyAttendanceSummary> findTop10ByOrderByIdDesc(); // ★★★ 追加: 最新10件を取得するメソッド ★★★

    /**
     * 指定された従業員IDと日付の勤怠集計レコードを検索します。
     *
     * 【機能】
     * employeeId と workDate が完全に一致する勤怠サマリーレコードを1件検索します。
     *
     * 【注意事項】
     * UPSERT処理の際に、既存のレコードをチェックするために使用されます。
     *
     * @param employeeId 従業員ID
     * @param workDate   勤務日
     * @return DailyAttendanceSummaryエンティティ（Optional）
     */
    Optional<DailyAttendanceSummary> findByEmployeeIdAndWorkDate(String employeeId, LocalDate workDate);

    /**
     * 指定された期間内かつ指定ステータスに該当する勤怠サマリーを検索します。
     *
     * 【機能】
     * workDate が startDate から endDate の間（両端含む）であり、かつステータスが statuses
     * リストに含まれるレコードをすべて取得します。
     *
     * 【注意事項】
     * 主に承認対象のデータや確定済みのデータなど、特定の状態のデータをフィルタリングするのに使用されます。
     *
     * @param startDate 期間開始日
     * @param endDate   期間終了日
     * @param statuses  検索対象とするステータス（例: "FINALIZED", "APPROVED"）
     * @return 条件に一致するDailyAttendanceSummaryエンティティのリスト
     */
    List<DailyAttendanceSummary> findByWorkDateBetweenAndStatusIn(LocalDate startDate, LocalDate endDate,
            List<String> statuses);

    /**
     * 指定された期間内の勤怠サマリーをすべて取得します。
     *
     * 【機能】
     * workDate が startDate から endDate の間（両端含む）に該当する勤怠サマリーレコードをすべて取得します。
     *
     * 【注意事項】
     * 従業員IDによる絞り込みは行いません。
     *
     * @param startDate 期間開始日
     * @param endDate   期間終了日
     * @return 条件に一致するDailyAttendanceSummaryエンティティのリスト
     */
    List<DailyAttendanceSummary> findByWorkDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 指定された従業員IDと期間内の勤怠サマリーを取得します。
     *
     * 【機能】
     * 指定された employeeId を持ち、かつ workDate が startDate から endDate
     * の間（両端含む）に該当するレコードを取得します。
     *
     * 【注意事項】
     * 特定の従業員の月次勤務表作成などに使用されます。
     *
     * @param employeeId 従業員ID
     * @param startDate  期間開始日
     * @param endDate    期間終了日
     * @return 条件に一致するDailyAttendanceSummaryエンティティのリスト
     */
    List<DailyAttendanceSummary> findByEmployeeIdAndWorkDateBetween(String employeeId, LocalDate startDate,
            LocalDate endDate);
}