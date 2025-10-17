package com.example.attendance.attendance_app.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
// ★修正: AllArgsConstructorは削除または調整が必要です。手動でコンストラクタを定義します。
public class PayrollDto {
    private String employeeId;
    private String employeeName;

    // 労働時間の集計
    private Double totalHours;
    private Double overtimeHours;
    private Double lateNightHours;

    // 支給額
    private Double basePaySalary; // 基本給
    private Double overtimePay; // 時間外手当額
    private Double lateNightPay; // 深夜手当額
    private Double totalGrossPay; // 支給合計額

    // 控除額
    private Double healthInsuranceFee; // 健康保険料
    private Double pensionFee; // 厚生年金保険料
    private Double employmentInsuranceFee; // 雇用保険料
    // ※ 所得税/住民税などは複雑なため、シンプル化のためここでは省略します。
    private Double incomeTax; // 所得税
    private Double residentTax; // 住民税
    private Double totalDeduction; // 控除合計額
    private Double netPay; // 差引支給額

    // ★修正: 既存のコンストラクタを再定義し、新しいフィールドに対応させます。★
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
        this.totalDeduction = 0.0;

        // 初期計算値として支給総額と差引支給額を設定 (後でサービスで再計算される)
        this.totalGrossPay = calculatedSalary;
        this.netPay = calculatedSalary;
    }
}