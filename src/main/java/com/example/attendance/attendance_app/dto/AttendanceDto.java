package com.example.attendance.attendance_app.dto;

import lombok.Data;
import java.time.OffsetDateTime;

/**
 * 個々の打刻（タイムスタンプ）の情報を保持するDTOです。
 *
 * 【用途】
 * 主に打刻ログの記録時や、最新打刻情報の取得時に利用されます。
 */
@Data
public class AttendanceDto {
    /**
     * 従業員ID。
     */
    private String employeeId;

    /**
     * 打刻が発生した日時（タイムゾーン情報付き）。
     */
    private OffsetDateTime stampTime;

    /**
     * 打刻種別（例: IN, OUT, BREAK_START, BREAK_END）。
     */
    private String stampType;

    /**
     * 打刻に関する備考、またはメモ。
     */
    private String note;
}