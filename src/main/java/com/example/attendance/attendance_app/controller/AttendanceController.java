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
     * 勤怠情報を登録します。
     *
     * 【注意事項】
     * バリデーションエラー時は400を返します。
     *
     * @param request 勤怠リクエスト
     * @return 登録結果メッセージ
     */
    @PostMapping("/stamp")
    public ResponseEntity<String> recordAttendance(@RequestBody AttendanceRequest request) {
        Attendance savedAttendance = attendanceService.recordAttendance(request);

        String formattedTimestamp = savedAttendance.getStampTime()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String message = String.format(
                "%sさんの「%s」を記録しました。(%s)",
                request.getName(),
                savedAttendance.getStampType(),
                formattedTimestamp);

        return ResponseEntity.ok(message);
    }

    /**
     * 最新勤怠取得APIのエンドポイントです.
     *
     * 【機能】
     * 指定従業員の最新勤怠情報を返却します。
     *
     * 【注意事項】
     * 該当データがない場合は404を返します。
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
