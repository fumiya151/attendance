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
     * 【機能】
     * 指定従業員の最新打刻状態から、次に有効な打刻種別（出勤・退勤・休憩開始・休憩終了）を判定しリストで返します。
     *
     * 【注意事項】
     * 該当データがない場合は「出勤」のみ有効となります。
     *
     * @param employeeId 従業員ID
     * @return 有効な打刻種別リスト
     */
    @GetMapping("/next-available/{employeeId}")
    public List<String> getNextAvailableStampTypes(@PathVariable String employeeId) {
        // Serviceからの応答をそのままJSONリストとして返す
        return attendanceService.getNextAvailableStampTypes(employeeId);
    }

    /**
     * 勤怠登録APIのエンドポイントです.
     *
     * 【機能】
     * 勤怠情報を登録します。打刻が「退勤」の場合、サービス層で日次集計（daily_attendance_summaryへの登録/更新）が実行されます。
     *
     * 【注意事項】
     * バリデーションエラー時は400を返します。
     *
     * @param request 勤怠リクエスト
     * @return 登録結果メッセージ
     */
    @PostMapping("/stamp")
    public ResponseEntity<String> recordAttendance(@RequestBody AttendanceRequest request) {
        // Serviceを呼び出し、attendanceテーブルに生ログを登録し、
        // 退勤の場合は daily_attendance_summary の登録・更新も実行する
        Attendance savedAttendance = attendanceService.recordAttendance(request);

        // タイムゾーン情報を含まないシンプルなフォーマットで時刻を整形
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
        // このAPIは、getNextAvailableStampTypes では使用しないため、そのまま残します
        return attendanceService.getLatestAttendance(employeeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 全勤怠取得APIのエンドポイントです.
     *
     * 【機能】
     * データベースに存在する全ての勤怠記録（出勤、退勤、休憩など）を取得し、DTOリストとして返却します。
     *
     * 【注意事項】
     * 処理に時間がかかる可能性があるため、本番環境では期間指定やページネーションを推奨します。
     *
     * @return 全勤怠DTOリスト
     */
    @GetMapping("/logs")
    public List<AttendanceDto> getAttendanceLogs() {
        return attendanceService.getAllAttendanceLogs();
    }
}
