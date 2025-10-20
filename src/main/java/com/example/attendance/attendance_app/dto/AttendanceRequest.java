package com.example.attendance.attendance_app.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 従業員からの打刻リクエストを保持するDTOです。
 *
 * 【用途】
 * /api/attendance/stamp エンドポイントを通じて、打刻（IN/OUTなど）を行うために利用されます。
 */
@Data
public class AttendanceRequest {
    /**
     * 必須：打刻リクエストを行う従業員のID。
     */
    private String employeeId;

    /**
     * 必須：リクエストを行う従業員の氏名。
     * （Controllerのメッセージ生成で使用されます。）
     */
    private String name;

    /**
     * 必須：打刻の種別（例: "IN", "OUT", "BREAK_START"）。
     */
    private String stampType;

    /**
     * オプション：打刻を行う日付。
     * （手動修正や日を跨ぐ打刻の際に使用されます。）
     */
    private LocalDate stampDate;

    /**
     * オプション：打刻時刻。
     * （手動修正時などに使用されます。通常はサーバー側で現在時刻を決定します。）
     */
    private LocalTime stampTime;
}