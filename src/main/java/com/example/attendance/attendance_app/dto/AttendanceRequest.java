package com.example.attendance.attendance_app.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AttendanceRequest {

    // 必須：打刻リクエストを行う従業員のID
    private String employeeId;

    // 必須：リクエストを行う従業員の氏名 (Controllerのメッセージ生成で使用)
    private String name;

    // 必須：打刻の種別 (例: "IN", "OUT")
    // Controllerの savedAttendance.getStampType() と連携させるため、stampType に変更
    private String stampType;

    // オプション：打刻を行う日付 (手動修正や日を跨ぐ打刻の際などに使用)
    private LocalDate stampDate;

    // オプション：打刻時刻 (手動修正時などに使用。通常はサーバー側で現在時刻を決定)
    private LocalTime stampTime;
}