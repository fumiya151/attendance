package com.example.attendance.attendance_app.controller;

import com.example.attendance.attendance_app.model.DailyAttendanceSummary;
import com.example.attendance.attendance_app.service.DailyAttendanceSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/summaries")
@RequiredArgsConstructor
public class DailyAttendanceSummaryController {

    private final DailyAttendanceSummaryService summaryService;

    private static final String OPERATOR_HEADER = "X-Operator-Id";

    /**
     * 指定期間の勤怠サマリーを一括承認するAPIです.
     *
     * 【機能】
     * 期間内の'PENDING'状態のレコードを'APPROVED'に更新し、承認者IDと承認日時を記録します。
     * 給与計算を可能にするための重要なステップです。
     *
     * @param startDate  承認期間開始日 (YYYY-MM-DD)
     * @param endDate    承認期間終了日 (YYYY-MM-DD)
     * @param approverId 承認操作を行った従業員ID (ヘッダーから取得)
     * @return 承認されたレコード数を含むメッセージ
     */
    @PostMapping("/approve/batch")
    public ResponseEntity<Map<String, String>> approveSummariesBatch(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestHeader(OPERATOR_HEADER) String approverId) { // ★ 承認者IDを取得

        // サービス層の月次一括承認メソッドを呼び出す
        List<DailyAttendanceSummary> approvedSummaries = summaryService.approveSummariesByPeriod(
                startDate,
                endDate,
                approverId);

        int count = approvedSummaries.size();
        String message = String.format("%sから%sまでの勤怠サマリー%d件を承認しました。",
                startDate.toString(),
                endDate.toString(),
                count);

        return ResponseEntity.ok(Map.of("message", message, "approvedCount", String.valueOf(count)));
    }
}