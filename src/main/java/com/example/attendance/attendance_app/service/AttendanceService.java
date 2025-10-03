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

    public Optional<AttendanceDto> getLatestAttendance(Long employeeId) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startOfDay = now.toLocalDate().atStartOfDay(now.getOffset()).toOffsetDateTime();
        return attendanceRepository.findTopByEmployeeIdAndStampTimeBetweenOrderByStampTimeDesc(employeeId, startOfDay, now)
                .map(this::convertToDto);
    }

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
