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
         * 指定された従業員ID、期間における最新の打刻を取得します。
         *
         * 【機能】
         * employeeId と指定期間 (start から end) に該当する打刻レコードのうち、最も新しいもの（1件）を取得します。
         *
         * 【注意事項】
         * 主に打刻のIN/OUTの連続判定など、最新の打刻状態を確認するために使用されます。
         *
         * @param employeeId 従業員ID
         * @param start      検索開始日時（含む）
         * @param end        検索終了日時（含まない）
         * @return 該当する最新の打刻レコード（Optional）
         */
        Optional<Attendance> findTopByEmployeeEmployeeIdAndStampTimeBetweenOrderByStampTimeDesc(String employeeId,
                        OffsetDateTime start, OffsetDateTime end);

        /**
         * 指定された従業員IDの全打刻ログを取得します。
         *
         * 【機能】
         * employeeId に該当する全ての打刻レコードを、打刻日時の昇順で取得します。
         *
         * 【注意事項】
         * 期間によるフィルタリングは行われないため、全期間のログが返却されます。
         *
         * @param employeeId 従業員ID
         * @return 該当する全打刻レコードのリスト
         */
        List<Attendance> findByEmployeeEmployeeId(String employeeId);

        /**
         * 指定された期間内の打刻ログを取得します。
         *
         * 【機能】
         * 指定された従業員IDと期間 (startDateTime 以上、endDateTime 未満) に該当する打刻レコードを取得します。
         *
         * 【注意事項】
         * 期間の指定には、JPAの JPQL クエリを使用しています。
         *
         * @param employeeId    対象の従業員ID
         * @param startDateTime 検索開始日時（含む）
         * @param endDateTime   検索終了日時（含まない）
         * @return 該当する打刻レコードのリスト
         */
        @Query("SELECT a FROM Attendance a WHERE a.employee.employeeId = :employeeId AND a.stampTime >= :startDateTime AND a.stampTime < :endDateTime")
        List<Attendance> findByPeriod(
                        @Param("employeeId") String employeeId,
                        @Param("startDateTime") OffsetDateTime startDateTime,
                        @Param("endDateTime") OffsetDateTime endDateTime);

        /**
         * 全件の勤怠ログを打刻日時の降順（最新順）にソートして取得します。
         *
         * 【機能】
         * 全従業員の全ての打刻レコードを、最新の打刻日時順に並べ替えて取得します。
         *
         * 【注意事項】
         * 従業員IDによる絞り込みは行いません。
         *
         * @return 打刻日時降順にソートされた全打刻レコードのリスト
         */
        List<Attendance> findAllByOrderByStampTimeDesc();

        /**
         * 指定された勤務日と打刻種別において、最も古い打刻レコードを取得します。
         *
         * 【機能】
         * 従業員IDと期間に一致するレコードのうち、stampTypeが一致し、最も古いstampTimeを持つレコードを1件取得します。
         *
         * 【注意事項】
         * サマリー修正時、IN打刻のstampTimeを修正するために使用されます。
         *
         * @param employeeId    対象従業員ID
         * @param stampType     打刻種別 (例: "IN")
         * @param startDateTime 検索開始日時（含む）
         * @param endDateTime   検索終了日時（含まない）
         * @return 該当する最も古い打刻レコード（Optional）
         */
        Optional<Attendance> findTopByEmployeeEmployeeIdAndStampTypeAndStampTimeBetweenOrderByStampTimeAsc(
                        String employeeId,
                        String stampType,
                        OffsetDateTime startDateTime,
                        OffsetDateTime endDateTime);

        /**
         * 指定された勤務日と打刻種別において、最も新しい打刻レコードを取得します。
         *
         * 【機能】
         * 従業員IDと期間に一致するレコードのうち、stampTypeが一致し、最も新しいstampTimeを持つレコードを1件取得します。
         *
         * 【注意事項】
         * サマリー修正時、OUT打刻のstampTimeを修正するために使用されます。
         *
         * @param employeeId    対象従業員ID
         * @param stampType     打刻種別 (例: "OUT")
         * @param startDateTime 検索開始日時（含む）
         * @param endDateTime   検索終了日時（含まない）
         * @return 該当する最も新しい打刻レコード（Optional）
         */
        Optional<Attendance> findTopByEmployeeEmployeeIdAndStampTypeAndStampTimeBetweenOrderByStampTimeDesc(
                        String employeeId,
                        String stampType,
                        OffsetDateTime startDateTime,
                        OffsetDateTime endDateTime);
}