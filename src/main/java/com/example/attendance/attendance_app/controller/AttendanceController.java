package com.example.attendance.attendance_app.controller;

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

    // ★ 定数として操作者IDのヘッダー名を定義
    private static final String OPERATOR_HEADER = "X-Operator-Id";

    /**
     * 次に有効な打刻種別リストを返すAPI
     *
     * @param employeeId 従業員ID
     * @return 有効な打刻種別リスト
     */
    @GetMapping("/next-available/{employeeId}")
    public List<String> getNextAvailableStampTypes(@PathVariable String employeeId) {
        return attendanceService.getNextAvailableStampTypes(employeeId);
    }

    /**
     * 勤怠登録APIのエンドポイントです.
     *
     * 【機能】
     * 勤怠情報を登録します。打刻が「退勤」の場合、サービス層で日次集計が実行され、操作者IDが監査ログに記録されます。
     *
     * @param request    勤怠リクエスト
     * @param operatorId 勤怠操作を行った従業員ID (ヘッダーから取得)
     * @return 登録結果メッセージ
     */
    @PostMapping("/stamp")
    public ResponseEntity<String> recordAttendance(
            @RequestBody AttendanceRequest request,
            @RequestHeader(OPERATOR_HEADER) String operatorId) { // ★ 修正点: ヘッダーから operatorId を取得

        // ★ 修正点: Serviceに operatorId を渡す
        Attendance savedAttendance = attendanceService.recordAttendance(request, operatorId);

        // タイムゾーン情報を含まないシンプルなフォーマットで時刻を整形
        String formattedTimestamp = savedAttendance.getStampTime()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // Note: request.getName() は AttendanceRequest DTO に name フィールドがある前提です
        String message = String.format(
                "%sさんの「%s」を記録しました。(%s)",
                request.getName(), // AttendanceRequest DTOにnameフィールドがあることを前提
                savedAttendance.getStampType(),
                formattedTimestamp);

        return ResponseEntity.ok(message);
    }

    /**
     * 最新勤怠取得APIのエンドポイントです.
     *
     * @param employeeId 従業員ID
     * @return 最新勤怠DTO
     */
    @GetMapping("/latest/{employeeId}")
    public ResponseEntity<AttendanceDto> getLatestAttendance(@PathVariable String employeeId) {
        return attendanceService.getLatestAttendance(employeeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}