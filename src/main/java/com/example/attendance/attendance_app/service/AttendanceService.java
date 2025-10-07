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

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

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
        Optional<AttendanceDto> latestAttendanceOpt = getLatestAttendance(employeeId);
        if (latestAttendanceOpt.isEmpty()) {
            // 最初の打刻は「出勤」のみ有効
            return List.of(workSt);
        }
        String lastStampType = latestAttendanceOpt.get().getStampType();
        switch (lastStampType) {
            case workSt:
                return List.of(workEd, breakSt);
            case "退勤":
                // 1日に複数回の出退勤を許可する場合は「出勤」も有効
                return List.of(workSt);
            case "休憩開始":
                return List.of(breakEd);
            case "休憩終了":
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
        attendance.setStampTime(OffsetDateTime.now(ZoneId.of("Asia/Tokyo")));
        attendance.setStampType(request.getAttendanceType());

        return attendanceRepository.save(attendance);
    }

    /**
     * 最新の勤怠情報を取得するメソッドです.
     *
     * 【機能】
     * 指定従業員の最新勤怠情報を返します。
     *
     * 【注意事項】
     * 該当データがない場合は空を返します。
     *
     * @param employeeId 従業員ID
     * @return 最新勤怠DTO（Optional）
     */
    public Optional<AttendanceDto> getLatestAttendance(String employeeId) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startOfDay = now.toLocalDate().atStartOfDay(now.getOffset()).toOffsetDateTime();
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
        dto.setId(attendance.getId());
        dto.setEmployeeId(attendance.getEmployee().getEmployeeId());
        dto.setStampTime(attendance.getStampTime());
        dto.setStampType(attendance.getStampType());
        dto.setNote(attendance.getNote());
        return dto;
    }
}
