package com.example.attendance.attendance_app.service;

import com.example.attendance.attendance_app.model.DailyAttendanceSummary;
import com.example.attendance.attendance_app.repository.DailyAttendanceSummaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyAttendanceSummaryService {

    private final DailyAttendanceSummaryRepository summaryRepository;

    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_PENDING = "PENDING";

    /**
     * 指定期間内の勤怠サマリーレコードを「承認済み」に一括更新します。
     *
     * 【機能】
     * 1. 指定期間内の 'PENDING' 状態のレコードを全て取得します。
     * 2. 各レコードのステータスを 'APPROVED' に変更し、承認者IDと承認日時を設定します。
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
        // Note: findByWorkDateBetweenAndStatusIn メソッドがリポジトリにあることを前提
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

            // calculatedAt は @PreUpdate で自動更新されます
        }

        // 3. 一括保存（更新）
        return summaryRepository.saveAll(summariesToApprove);
    }
}