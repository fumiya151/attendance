package com.example.attendance.attendance_app.service;

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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    /**
     * 勤怠記録を登録するメソッドです.
     *
     * 【機能】
     * 勤怠リクエストを受け取り、勤怠情報を登録します。
     *
     *【注意事項】
     * リクエストのバリデーションを行います。
     *
     * @param request 勤怠リクエスト
     * @return 登録された勤怠エンティティ
     */
    public Attendance recordAttendance(AttendanceRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + request.getEmployeeId()));

        validateAttendanceRequest(request);

        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setStampTime(OffsetDateTime.now());
        attendance.setStampType(request.getAttendanceType());
        // 'note' is not provided in the request, so it will be null.

        return attendanceRepository.save(attendance);
    }

    /**
     * 勤怠リクエストのバリデーションを行うメソッドです.
     *
     * 【機能】
     * 勤怠リクエストの内容をチェックします。
     *
     *【注意事項】
     * 不正な場合は例外を投げます。
     *
     * @param request 勤怠リクエスト
     */
    private void validateAttendanceRequest(AttendanceRequest request) {
        Optional<AttendanceDto> latestAttendanceOpt = getLatestAttendance(request.getEmployeeId());
        String newStampType = request.getAttendanceType();

        if (latestAttendanceOpt.isEmpty()) {
            if (!newStampType.equals("出勤")) {
                throw new IllegalStateException("最初の打刻は「出勤」である必要があります。");
            }
            return;
        }

        String lastStampType = latestAttendanceOpt.get().getStampType();

        switch (lastStampType) {
            case "出勤":
                if (!newStampType.equals("退勤") && !newStampType.equals("休憩開始")) {
                    throw new IllegalStateException("「出勤」の後は「退勤」または「休憩開始」のみ可能です。");
                }
                break;
            case "退勤":
                 if (newStampType.equals("出勤")) {
                    // 1日に複数回の出退勤を許可する場合
                    return;
                }
                throw new IllegalStateException("本日は既に「退勤」済みです。");
            case "休憩開始":
                if (!newStampType.equals("休憩終了")) {
                    throw new IllegalStateException("「休憩開始」の後は「休憩終了」のみ可能です。");
                }
                break;
            case "休憩終了":
                if (!newStampType.equals("退勤") && !newStampType.equals("休憩開始")) {
                    throw new IllegalStateException("「休憩終了」の後は「退勤」または「休憩開始」のみ可能です。");
                }
                break;
            default:
                throw new IllegalStateException("不明な打刻状態です。");
        }
    }

    /**
     * 最新の勤怠情報を取得するメソッドです.
     *
     * 【機能】
     * 指定従業員の最新勤怠情報を返します。
     *
     *【注意事項】
     * 該当データがない場合は空を返します。
     *
     * @param employeeId 従業員ID
     * @return 最新勤怠DTO（Optional）
     */
    public Optional<AttendanceDto> getLatestAttendance(Long employeeId) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startOfDay = now.toLocalDate().atStartOfDay(now.getOffset()).toOffsetDateTime();
        return attendanceRepository.findTopByEmployeeIdAndStampTimeBetweenOrderByStampTimeDesc(employeeId, startOfDay, now)
                .map(this::convertToDto);
    }

    /**
     * 勤怠エンティティをDTOに変換するメソッドです.
     *
     * 【機能】
     * エンティティの各項目をDTOにセットします。
     *
     *【注意事項】
     * 特になし
     *
     * @param attendance 勤怠エンティティ
     * @return 勤怠DTO
     */
    private AttendanceDto convertToDto(Attendance attendance) {
        AttendanceDto dto = new AttendanceDto();
        dto.setId(attendance.getId());
        dto.setEmployeeId(attendance.getEmployee().getId());
        dto.setStampTime(attendance.getStampTime());
        dto.setStampType(attendance.getStampType());
        dto.setNote(attendance.getNote());
        return dto;
    }
}
