package com.example.attendance.attendance_app.service;

import com.example.attendance.attendance_app.model.DailyAttendanceSummary;
import com.example.attendance.attendance_app.model.Employee; // ★ 追加
import com.example.attendance.attendance_app.repository.DailyAttendanceSummaryRepository;
import com.example.attendance.attendance_app.repository.EmployeeRepository; // ★ 追加
import com.example.attendance.attendance_app.dto.DailyAttendanceSummaryDto; // ★ 追加
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map; // ★ 追加
import java.util.stream.Collectors; // ★ 追加

@Service
@RequiredArgsConstructor
public class DailyAttendanceSummaryService {

    private final DailyAttendanceSummaryRepository summaryRepository;
    private final EmployeeRepository employeeRepository; // ★ 従業員名取得のために注入

    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_PENDING = "PENDING";

    /**
     * 全期間の勤怠サマリーを従業員名情報と承認ステータス付きで取得します。
     *
     * 【機能】
     * DailyAttendanceSummaryとEmployee情報を結合し、フロントエンドのテーブル描画用DTOに変換します。
     *
     * @return DailyAttendanceSummaryDtoのリスト
     */
    public List<DailyAttendanceSummaryDto> getAllSummariesWithEmployeeInfo() {
        // 1. 全従業員情報を取得し、IDをキーとしたマップに変換 (高速検索のため)
        Map<String, String> employeeNamesById = employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getEmployeeId, Employee::getName));

        // 2. 全ての勤怠サマリーを取得
        // Note: 必要に応じて、ソート順をリポジトリに追加すると便利です (例: findByOrderByWorkDateDesc)
        List<DailyAttendanceSummary> allSummaries = summaryRepository.findAll();

        // 3. サマリーと従業員名を結合し、DTOに変換
        return allSummaries.stream()
                .map(summary -> {
                    DailyAttendanceSummaryDto dto = new DailyAttendanceSummaryDto();

                    // 基本情報
                    dto.setId(summary.getId());
                    dto.setEmployeeId(summary.getEmployeeId());
                    dto.setWorkDate(summary.getWorkDate());

                    // 従業員名をマップから取得
                    dto.setEmployeeName(employeeNamesById.getOrDefault(summary.getEmployeeId(), "不明な従業員"));

                    // 勤務情報
                    dto.setActualInTime(summary.getActualInTime());
                    dto.setActualOutTime(summary.getActualOutTime());
                    dto.setTotalBreakMinutes(summary.getTotalBreakMinutes());
                    dto.setTotalWorkMinutes(summary.getTotalWorkMinutes());

                    // ステータス情報
                    // Note: ログ整合性のチェックは別途生ログが必要だが、ここではダミー値をセット
                    dto.setLogStatus("集計済");
                    dto.setApprovalStatus(summary.getStatus()); // DBのPENDING/APPROVED/FINALIZEDをそのまま利用

                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 指定期間内の勤怠サマリーレコードを「承認済み」に一括更新します。
     *
     * @param startDate  承認期間開始日
     * @param endDate    承認期間終了日
     * @param approverId 承認操作を行った従業員ID (管理者/マネージャー)
     * @return 承認されたレコードのリスト
     */
    @Transactional
    public List<DailyAttendanceSummary> approveSummariesByPeriod(
            LocalDate startDate,
            LocalDate endDate,
            String approverId) {

        // 1. 指定期間内のPENDING状態のレコードを全て取得
        List<DailyAttendanceSummary> summariesToApprove = summaryRepository.findByWorkDateBetweenAndStatusIn(
                startDate,
                endDate,
                List.of(STATUS_PENDING));

        OffsetDateTime now = OffsetDateTime.now();

        // 2. 各レコードの監査フィールドとステータスを更新
        for (DailyAttendanceSummary summary : summariesToApprove) {
            // ステータスをAPPROVEDに変更
            summary.setStatus(STATUS_APPROVED);

            // 承認情報を設定
            summary.setApprovedById(approverId);
            summary.setApprovedAt(now);

            // 承認も一種の更新操作として記録
            summary.setUpdatedById(approverId);
        }

        // 3. 一括保存（更新）
        return summaryRepository.saveAll(summariesToApprove);
    }
}