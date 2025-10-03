package com.example.attendance.attendance_app.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private String role;
}
