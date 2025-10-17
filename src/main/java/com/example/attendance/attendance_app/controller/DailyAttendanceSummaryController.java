package com.example.attendance.attendance_app.controller;

import com.example.attendance.attendance_app.dto.DailyAttendanceSummaryDto;
import com.example.attendance.attendance_app.dto.MonthlySummaryDto;
import com.example.attendance.attendance_app.service.DailyAttendanceSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/summaries")
@RequiredArgsConstructor
public class DailyAttendanceSummaryController {

    private final DailyAttendanceSummaryService summaryService;

    private static final String OPERATOR_HEADER = "X-Operator-Id";

    /**
     * 指定されたIDの勤怠サマリーレコードを単体で取得するAPIエンドポイントです。
     *
     * 【機能】
     * パス変数で渡されたIDに一致する DailyAttendanceSummaryDto を返却します。
     *
     * 【注意事項】
     * 主に勤怠修正画面の初期データ表示のために使用されます。データが見つからない場合は 404 Not Found を返します。
     *
     * @param id 勤怠サマリーID (主キー)
     * @return DailyAttendanceSummaryDto
     */
    @GetMapping("/{id}")
    public ResponseEntity<DailyAttendanceSummaryDto> getSummaryById(@PathVariable Long id) {
        DailyAttendanceSummaryDto dto = summaryService.getSummaryDtoById(id);
        return ResponseEntity.ok(dto);
    }

    /**
     * 指定された勤怠サマリーを更新するAPIエンドポイントです。
     *
     * 【機能】
     * 修正された出勤・退勤・休憩時間を受信し、Service層で再集計ロジックを実行して勤怠サマリーを更新します。
     *
     * 【注意事項】
     * 修正操作を行ったオペレーターのIDがヘッダーから取得され、更新者として記録されます。
     *
     * @param id         勤怠サマリーID (主キー)
     * @param requestDto 修正後の勤怠データを含むDTO
     * @param operatorId HTTPヘッダー（X-Operator-Idを取得）
     * @return 更新後の DailyAttendanceSummaryDto
     */
    @PutMapping("/{id}")
    public ResponseEntity<DailyAttendanceSummaryDto> updateSummary(
            @PathVariable Long id,
            @RequestBody DailyAttendanceSummaryDto requestDto,
            @RequestHeader(OPERATOR_HEADER) String operatorId) {

        DailyAttendanceSummaryDto updatedDto = summaryService.updateSummary(id, requestDto, operatorId);
        return ResponseEntity.ok(updatedDto);
    }

    /**
     * 指定された年月の月次勤怠集計データを取得するAPIエンドポイントです。
     *
     * 【機能】
     * 指定された年月（YYYY-MM）に基づき、その月の全従業員の集計情報（総労働時間、平均残業時間など）を返却します。
     *
     * 【注意事項】
     * yearMonthの形式が不正な場合、HTTPステータス 400 Bad Request とエラーメッセージを返します。
     *
     * @param yearMonth 対象年月 (YYYY-MM形式)
     * @return 月次集計情報を含む MonthlySummaryDto
     */
    @GetMapping("/monthly")
    public ResponseEntity<?> getMonthlySummary(@RequestParam String yearMonth) {
        try {
            YearMonth ym = YearMonth.parse(yearMonth);
            MonthlySummaryDto summary = summaryService.getMonthlySummary(ym);
            return ResponseEntity.ok(summary);
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body("Invalid yearMonth format. Please use YYYY-MM.");
        }
    }

    /**
     * 全期間の勤怠サマリーを従業員名情報付きで取得するAPIです。
     *
     * 【機能】
     * データベースに存在する全ての勤怠サマリーレコードを取得し、関連する従業員名を結合したDTOリストを返却します。
     *
     * 【注意事項】
     * 大量のデータが存在する場合、パフォーマンスに影響を与える可能性があります。
     *
     * @return DailyAttendanceSummaryDtoのリスト
     */
    @GetMapping
    public List<DailyAttendanceSummaryDto> getAllSummariesForDisplay() {
        // Service層でEmployee情報と結合されたDTOを取得する
        return summaryService.getAllSummariesWithEmployeeInfo();
    }

    /**
     * 勤怠サマリーを従業員IDで従業員名情報付きで取得するAPIです。
     *
     * 【機能】
     * データベースに存在する全ての勤怠サマリーレコードを取得し、関連する従業員名を結合したDTOリストを返却します。
     *
     * 【注意事項】
     * 大量のデータが存在する場合、パフォーマンスに影響を与える可能性があります。
     *
     * @return DailyAttendanceSummaryDtoのリスト
     */
    @GetMapping("/emplimitsummaries")
    public List<DailyAttendanceSummaryDto> getLimitSummariesForDisplay(String employeeId) {
        // Service層でEmployee情報と結合されたDTOを取得する
        return summaryService.getLimitSummariesWithEmployeeInfo(employeeId);
    }

    /**
     * 最新の勤怠サマリー10件を新しい順に取得するAPIエンドポイントです。
     *
     * 【機能】
     * データベースから最新の**10件**の勤怠サマリーレコードを取得し、リストとして返却します。
     *
     * 【注意事項】
     * 主にダッシュボードなどの概要表示のために使用されます。
     *
     * @return 最新10件の DailyAttendanceSummaryDto リスト
     */
    @GetMapping("/limitsummaries")
    public ResponseEntity<List<DailyAttendanceSummaryDto>> getLatest10DailyAttendanceSummaries() {
        // Service層の新しいメソッドを呼び出す
        List<DailyAttendanceSummaryDto> latestSummaries = summaryService.findLatest10DailySummaries();
        return ResponseEntity.ok(latestSummaries);
    }

    /**
     * POST /api/summaries/approve/list
     * 勤怠サマリーIDと期間を受け取り、一括承認を実行します。（検索結果と期間による二重チェックに対応）
     *
     * 【機能】
     * リクエストで指定されたIDリストの勤怠サマリーに対し、承認者のIDと指定された期間を検証しつつ、承認状態を更新します。
     *
     * 【注意事項】
     * 単体承認の場合も、ID 1件と期間を送信することでこのエンドポイントが処理します。検証エラーや業務ロジックエラーは400を返します。
     *
     * @param operatorId  HTTPヘッダー（X-Operator-Idを取得）
     * @param requestBody JSONボディ（summaryIds, startDate, endDate）
     * @return 承認されたレコード数を含む応答 (JSON形式: message, approvedCount)
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