package com.example.attendance.attendance_app.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 従業員の月次給与計算結果（支給と控除の内訳）を保持するDTOです。
 *
 * 【用途】
 * 給与計算サービスの結果をクライアントに返却する際や、明細表示のために利用されます。
 */
@Data
@NoArgsConstructor
public class PayrollDto {
    /**
     * 従業員ID。
     */
    private String employeeId;

    /**
     * 従業員名。
     */
    private String employeeName;

    // 労働時間の集計

    /**
     * 総労働時間（時間単位）。
     */
    private Double totalHours;

    /**
     * 総残業時間（時間単位）。
     */
    private Double overtimeHours;

    /**
     * 総深夜勤務時間（時間単位）。
     */
    private Double lateNightHours;

    // 支給額

    /**
     * 基本給額（計算された所定労働時間分の賃金）。
     */
    private Double basePaySalary;

    /**
     * 時間外手当額（残業代）。
     */
    private Double overtimePay;

    /**
     * 深夜手当額。
     */
    private Double lateNightPay;

    /**
     * 支給合計額（基本給＋各種手当）。
     */
    private Double totalGrossPay;

    // 控除額

    /**
     * 健康保険料。
     */
    private Double healthInsuranceFee;

    /**
     * 厚生年金保険料。
     */
    private Double pensionFee;

    /**
     * 雇用保険料。
     */
    private Double employmentInsuranceFee;

    /**
     * 所得税（源泉徴収額）。
     */
    private Double incomeTax;

    /**
     * 住民税（特別徴収）。
     */
    private Double residentTax;

    /**
     * 控除合計額。
     */
    private Double totalDeduction;

    /**
     * 差引支給額（手取り額）。
     */
    private Double netPay;

    /**
     * 給与計算結果の初期値を設定するためのコンストラクタです。
     *
     * 【機能】
     * 労働時間情報と基本給（calculatedSalary）を受け取り、他の手当や控除額を0.0で初期化し、
     * 総支給額と差引支給額を初期基本給で設定します。
     *
     * @param employeeId       従業員ID
     * @param employeeName     従業員名
     * @param totalHours       総労働時間
     * @param overtimeHours    総残業時間
     * @param lateNightHours   総深夜勤務時間
     * @param calculatedSalary 計算された基本給額
     */
    public PayrollDto(String employeeId, String employeeName, Double totalHours,
            Double overtimeHours, Double lateNightHours, Double calculatedSalary) {
        this.employeeId = employeeId;
        this.employeeName = employeeName;
        this.totalHours = totalHours;
        this.overtimeHours = overtimeHours;
        this.lateNightHours = lateNightHours;
        this.basePaySalary = calculatedSalary; // 旧 calculatedSalary を基本給として設定

        // 初期値設定
        this.overtimePay = 0.0;
        this.lateNightPay = 0.0;
        this.healthInsuranceFee = 0.0;
        this.pensionFee = 0.0;
        this.employmentInsuranceFee = 0.0;
        this.incomeTax = 0.0;
        this.residentTax = 0.0;
        this.totalDeduction = 0.0;

        // 初期計算値として支給総額と差引支給額を設定 (後でサービスで再計算される)
        this.totalGrossPay = calculatedSalary;
        this.netPay = calculatedSalary;
    }
}