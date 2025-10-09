package com.example.attendance.attendance_app.controller;

import com.example.attendance.attendance_app.dto.DailyAttendanceSummaryDto; // ★ 追加
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
         * 全期間の勤怠サマリーを従業員名情報付きで取得するAPIです。
         *
         * 【機能】
         * 勤怠ログ/修正画面のメインテーブル表示に使用されます。
         *
         * @return DailyAttendanceSummaryDtoのリスト
         */
        @GetMapping
        public List<DailyAttendanceSummaryDto> getAllSummariesForDisplay() {
                // Service層でEmployee情報と結合されたDTOを取得する
                return summaryService.getAllSummariesWithEmployeeInfo();
        }

        /**
         * 指定期間の勤怠サマリーを一括承認するAPIです.
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
                        @RequestHeader(OPERATOR_HEADER) String approverId) {

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