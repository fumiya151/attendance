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

        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setStampTime(OffsetDateTime.now());
        attendance.setStampType(request.getAttendanceType());
        // 'note' is not provided in the request, so it will be null.

        return attendanceRepository.save(attendance);
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
