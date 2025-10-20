package com.example.attendance.attendance_app.service;

import com.example.attendance.attendance_app.model.Employee;
import com.example.attendance.attendance_app.model.EmployeeRole;
import com.example.attendance.attendance_app.repository.LoginRepository;
import com.example.attendance.attendance_app.repository.EmployeeRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // トランザクションはそのまま残します

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final LoginRepository loginRepository;
    private final EmployeeRoleRepository employeeRoleRepository;

    @Override
    @Transactional // LazyInitializationException対策
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Employee employee = loginRepository.findByEmployeeId(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with employeeId: " + username));

        List<EmployeeRole> employeeRoles = employeeRoleRepository.findByEmployeeId(employee.getEmployeeId());
        List<SimpleGrantedAuthority> authorities = employeeRoles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRole().getRoleCode()))
                .collect(Collectors.toList());

        return new User(employee.getEmployeeId(), employee.getPassword(), authorities);
    }
}