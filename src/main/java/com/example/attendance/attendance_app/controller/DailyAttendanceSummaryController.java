package com.example.attendance.attendance_app.controller;

import com.example.attendance.attendance_app.dto.DailyAttendanceSummaryDto;
import com.example.attendance.attendance_app.service.DailyAttendanceSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
         * @return DailyAttendanceSummaryDtoのリスト
         */
        @GetMapping
        public List<DailyAttendanceSummaryDto> getAllSummariesForDisplay() {
                // Service層でEmployee情報と結合されたDTOを取得する
                return summaryService.getAllSummariesWithEmployeeInfo();
        }

        // ★ 削除: /approve/batch エンドポイントは削除されました（機能統合のため） ★

        /**
         * POST /api/summaries/approve/list
         * 勤怠サマリーIDと期間を受け取り、一括承認を実行します。（検索結果と期間による二重チェックに対応）
         * 単体承認の場合も、ID 1件と期間を送信することでこのエンドポイントが処理します。
         *
         * @param operatorId  HTTPヘッダー（X-Operator-Idを取得）
         * @param requestBody JSONボディ（summaryIds, startDate, endDate）
         * @return 承認されたレコード数を含む応答
         */
        @PostMapping("/approve/list")
        @SuppressWarnings("unchecked")
        public ResponseEntity<Map<String, Object>> batchApproveSummaries(
                        @RequestHeader(OPERATOR_HEADER) String operatorId,
                        @RequestBody Map<String, Object> requestBody) {

                // JSONから各データ型を抽出・変換
                List<Long> summaryIds = (List<Long>) requestBody.get("summaryIds");
                String startDateStr = (String) requestBody.get("startDate");
                String endDateStr = (String) requestBody.get("endDate");

                if (summaryIds == null || summaryIds.isEmpty() || startDateStr == null || endDateStr == null) {
                        return ResponseEntity.badRequest()
                                        .body(Map.of("message", "承認対象のIDリストまたは期間が提供されていません。", "approvedCount", 0));
                }

                // String型の日付をLocalDateに変換
                LocalDate startDate = LocalDate.parse(startDateStr);
                LocalDate endDate = LocalDate.parse(endDateStr);

                try {
                        // Service層でIDリスト、開始日、終了日による二重チェック承認処理を呼び出す
                        int approvedCount = summaryService.approveSummariesByIds(summaryIds, startDate, endDate,
                                        operatorId);

                        // 成功応答を返す。JavaScript側が期待する形式（approvedCount）
                        return ResponseEntity.ok(Map.of(
                                        "message", approvedCount + "件の勤怠サマリーを承認しました。",
                                        "approvedCount", approvedCount));
                } catch (RuntimeException e) {
                        // サービス層での例外（例: 管理者IDが見つからない）をキャッチし、400エラーとして返す
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(Map.of("message", e.getMessage(), "approvedCount", 0));
                }
        }
}