package com.example.attendance.attendance_app.service;

import com.example.attendance.attendance_app.dto.PayrollDto;
import com.example.attendance.attendance_app.model.Attendance;
import com.example.attendance.attendance_app.model.Employee;
import com.example.attendance.attendance_app.repository.AttendanceRepository;
import com.example.attendance.attendance_app.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class PayrollService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    public List<PayrollDto> calculatePayroll() {
        List<Employee> employees = employeeRepository.findAll();
        List<PayrollDto> payrolls = new ArrayList<>();

        for (Employee employee : employees) {
            // 時給が設定されていない、または0以下の従業員はスキップ
            if (employee.getHourlyWage() == null || employee.getHourlyWage() <= 0) {
                continue;
            }

            List<Attendance> attendances = attendanceRepository.findByEmployeeId(employee.getId());
            
            // 出勤と退勤のペアを処理
            double totalHours = 0.0;
            OffsetDateTime clockInTime = null;

            // 打刻時間でソート
            attendances.sort((a1, a2) -> a1.getStampTime().compareTo(a2.getStampTime()));

            for (Attendance attendance : attendances) {
                if ("clock-in".equals(attendance.getStampType())) {
                    // 既に出勤打刻がある場合は、最後のものを採用
                    clockInTime = attendance.getStampTime();
                } else if ("clock-out".equals(attendance.getStampType()) && clockInTime != null) {
                    Duration duration = Duration.between(clockInTime, attendance.getStampTime());
                    totalHours += duration.toMinutes() / 60.0;
                    clockInTime = null; // ペアをリセット
                }
            }

            double calculatedSalary = totalHours * employee.getHourlyWage();

            payrolls.add(new PayrollDto(
                employee.getId(),
                employee.getName(),
                // 小数点第2位で四捨五入
                Math.round(totalHours * 100.0) / 100.0,
                Double.valueOf(Math.round(calculatedSalary))
            ));
        }

        return payrolls;
    }
}
