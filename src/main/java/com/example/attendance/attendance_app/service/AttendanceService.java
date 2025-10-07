package com.example.attendance.attendance_app.service;

import java.util.Optional;
import java.util.List;
import com.example.attendance.attendance_app.dto.AttendanceRequest;
import com.example.attendance.attendance_app.dto.AttendanceDto;
import com.example.attendance.attendance_app.model.Attendance;
import com.example.attendance.attendance_app.model.Employee;
import com.example.attendance.attendance_app.repository.AttendanceRepository;
import com.example.attendance.attendance_app.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    // JST (UTC+9) タイムゾーンを定義
    private static final ZoneId JST_ZONE = ZoneId.of("Asia/Tokyo");

    // 打刻種別の定数
    private final String workSt = "出勤";
    private final String workEd = "退勤";
    private final String breakSt = "休憩開始";
    private final String breakEd = "休憩終了";

    /**
     * 次に有効な打刻種別リストを返すメソッドです.
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
    public List<String> getNextAvailableStampTypes(String employeeId) {
        // getLatestAttendance は「本日中の」最新打刻を取得する
        Optional<AttendanceDto> latestAttendanceOpt = getLatestAttendance(employeeId);

        if (latestAttendanceOpt.isEmpty()) {
            // 本日中の打刻がない場合、必ず「出勤」が有効
            return List.of(workSt);
        }

        String lastStampType = latestAttendanceOpt.get().getStampType();
        switch (lastStampType) {
            case workSt: // 出勤
                return List.of(workEd, breakSt);
            case workEd: // 退勤
                // 本日の勤務は終了とみなし、次の出勤を許可する（翌日以降の打刻用）
                return List.of(workSt);
            case breakSt: // 休憩開始
                return List.of(breakEd);
            case breakEd: // 休憩終了
                return List.of(workEd, breakSt);
            default:
                return List.of();
        }
    }

    /**
     * 勤怠記録を登録するメソッドです.
     *
     * 【機能】
     * 勤怠リクエストを受け取り、勤怠情報を登録します。
     *
     * 【注意事項】
     * 特になし
     *
     * @param request 勤怠リクエスト
     * @return 登録された勤怠エンティティ
     */
    @Transactional
    public Attendance recordAttendance(AttendanceRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + request.getEmployeeId()));

        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        // JST (Asia/Tokyo) タイムゾーンで現在時刻を取得し、DBに保存
        attendance.setStampTime(OffsetDateTime.now(JST_ZONE));
        attendance.setStampType(request.getAttendanceType());

        return attendanceRepository.save(attendance);
    }

    /**
     * 最新の勤怠情報を取得するメソッドです.
     *
     * 【機能】
     * 指定従業員の最新勤怠情報を返します。（本日中の最新打刻に絞る）
     *
     * 【注意事項】
     * 該当データがない場合は空を返します。
     *
     * @param employeeId 従業員ID
     * @return 最新勤怠DTO（Optional）
     */
    public Optional<AttendanceDto> getLatestAttendance(String employeeId) {
        // JST (Asia/Tokyo) を基準に「今」の時刻を取得
        OffsetDateTime now = OffsetDateTime.now(JST_ZONE);

        // 今日の日付の開始時刻（00:00:00 JST）をOffsetDateTimeとして取得
        OffsetDateTime startOfDay = now.toLocalDate().atStartOfDay(JST_ZONE).toOffsetDateTime();

        // Repository のメソッド名が
        // findTopByEmployeeEmployeeIdAndStampTimeBetweenOrderByStampTimeDesc であることを前提
        return attendanceRepository
                .findTopByEmployeeEmployeeIdAndStampTimeBetweenOrderByStampTimeDesc(employeeId, startOfDay, now)
                .map(this::convertToDto);
    }

    /**
     * 勤怠エンティティをDTOに変換するメソッドです.
     *
     * 【機能】
     * エンティティの各項目をDTOにセットします。
     *
     * 【注意事項】
     * 特になし
     *
     * @param attendance 勤怠エンティティ
     * @return 勤怠DTO
     */
    private AttendanceDto convertToDto(Attendance attendance) {
        AttendanceDto dto = new AttendanceDto();
        dto.setEmployeeId(attendance.getEmployee().getEmployeeId());
        dto.setStampTime(attendance.getStampTime());
        dto.setStampType(attendance.getStampType());
        dto.setNote(attendance.getNote());
        return dto;
    }

    /**
     * 全ての勤怠ログを取得するメソッドです。
     *
     * 【機能】
     * データベースに存在する全ての勤怠記録を、打刻日時の新しい順（降順）にソートし、DTOリストとして返却します。
     *
     * @return 全勤怠DTOリスト
     */
    public List<AttendanceDto> getAllAttendanceLogs() {
        return attendanceRepository.findAllByOrderByStampTimeDesc().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }
}