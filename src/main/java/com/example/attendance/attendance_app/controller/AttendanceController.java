package com.example.attendance.attendance_app.controller;

import com.example.attendance.attendance_app.dto.AggregatedAttendanceSummaryDto;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.example.attendance.attendance_app.dto.AttendanceRequest;
import com.example.attendance.attendance_app.dto.AttendanceDto;
import com.example.attendance.attendance_app.model.Attendance;
import com.example.attendance.attendance_app.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

        private final AttendanceService attendanceService;

        // 操作者IDのヘッダー名を定義
        private static final String OPERATOR_HEADER = "X-Operator-Id";

        /**
         * 指定された期間の全従業員の集計勤怠サマリーを取得するAPIエンドポイントです。
         *
         * 【機能】
         * 指定された開始日と終了日を含む期間の勤怠データを集計し、従業員ごとのサマリーリストを返却します。
         *
         * 【注意事項】
         * 開始日と終了日が同じ日である場合、その日一日のサマリーが返却されます。
         *
         * @param startDate 集計期間の開始日 (yyyy-MM-dd 形式)
         * @param endDate   集計期間の終了日 (yyyy-MM-dd 形式)
         * @return 集計された勤怠サマリーDTOのリスト
         */
        @GetMapping("/summary")
        public ResponseEntity<List<AggregatedAttendanceSummaryDto>> getAggregatedAttendanceSummary(
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
                List<AggregatedAttendanceSummaryDto> summaries = attendanceService.getAggregatedAttendanceSummary(
                                startDate,
                                endDate);
                return ResponseEntity.ok(summaries);
        }

        /**
         * 勤怠登録APIのエンドポイントです。
         *
         * 【機能】
         * リクエストボディの打刻情報とヘッダーの操作者IDに基づき、勤怠データをデータベースに記録します。
         * 成功した場合、記録された打刻情報を含むメッセージを返却します。
         *
         * 【注意事項】
         * 処理の前に、打刻種別（IN, OUTなど）と従業員IDの組み合わせが業務ロジック上適切であるか検証されます。
         *
         * @param request    登録する打刻情報を含むリクエストDTO
         * @param operatorId 勤怠を操作しているユーザーのID (HTTPヘッダー: X-Operator-Id から取得)
         * @return 登録結果のメッセージ（HTTPステータス 200 OK）
         */
        @PostMapping("/stamp")
        public ResponseEntity<String> recordAttendance(
                        @RequestBody AttendanceRequest request,
                        @RequestHeader(OPERATOR_HEADER) String operatorId) {
                Attendance savedAttendance = attendanceService.recordAttendance(request, operatorId);

                // タイムゾーン情報を含まないシンプルなフォーマットで時刻を整形
                String formattedTimestamp = savedAttendance.getStampTime()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                // Note: request.getName() は AttendanceRequest DTO に name フィールドがある前提です
                String message = String.format(
                                "%sさんの「%s」を記録しました。(%s)",
                                request.getName(),
                                savedAttendance.getStampType(),
                                formattedTimestamp);

                return ResponseEntity.ok(message);
        }

        /**
         * 最新勤怠取得APIのエンドポイントです。
         *
         * 【機能】
         * 指定された従業員IDのデータベースに記録された最新の打刻情報を取得します。
         *
         * 【注意事項】
         * 打刻情報が存在しない場合、HTTPステータスコード 404 Not Found を返却します。
         *
         * @param employeeId 最新の勤怠を取得する対象の従業員ID
         * @return 最新の勤怠情報を含むDTO、または 404 Not Found
         */
        @GetMapping("/latest/{employeeId}")
        public ResponseEntity<AttendanceDto> getLatestAttendance(@PathVariable String employeeId) {
                return attendanceService.getLatestAttendance(employeeId)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.notFound().build());
        }

        /**
         * 次に実行可能な打刻種別を取得するAPIのエンドポイントです。
         *
         * 【機能】
         * 指定された従業員IDの最新の打刻状態に基づき、次にシステムが受け付けることができる打刻種別（例：最新がINなら次はOUT）のリストを返却します。
         *
         * 【注意事項】
         * 従業員の最新の打刻が存在しない場合、全ての打刻種別が可能なものとしてリストに含まれます。
         *
         * @param employeeId 従業員ID
         * @return 次に可能な打刻種別のリスト
         */
        @GetMapping("/next-available/{employeeId}")
        public ResponseEntity<List<String>> getNextAvailableStampTypes(@PathVariable String employeeId) {
                List<String> availableTypes = attendanceService.getNextAvailableStampTypes(employeeId);
                return ResponseEntity.ok(availableTypes);
        }
}