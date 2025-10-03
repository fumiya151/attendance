package com.example.attendance.attendance_app.service;

import com.example.attendance.attendance_app.dto.EmployeeDto;
import com.example.attendance.attendance_app.model.Employee;
import com.example.attendance.attendance_app.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public List<EmployeeDto> getEmployees() {
        return employeeRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public boolean login(String employeeCode, String password) {
        Optional<Employee> employeeOptional = employeeRepository.findByEmployeeCode(employeeCode);
        if (employeeOptional.isPresent()) {
            Employee employee = employeeOptional.get();
            return password.equals(employee.getPassword());
        }
        return false;
    }

    private EmployeeDto convertToDto(Employee employee) {
        EmployeeDto dto = new EmployeeDto();
        dto.setId(employee.getId());
        dto.setEmployeeCode(employee.getEmployeeCode());
        dto.setName(employee.getName());
        return dto;
    }
}
