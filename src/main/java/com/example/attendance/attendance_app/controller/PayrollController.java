package com.example.attendance.attendance_app.controller;

import com.example.attendance.attendance_app.dto.PayrollDto;
import com.example.attendance.attendance_app.service.PayrollService;
import lombok.RequiredArgsConstructor; // コンストラクタインジェクションのために追加
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat; // 日付フォーマットのために追加
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam; // クエリパラメータのために追加
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/payroll")
@RequiredArgsConstructor // コンストラクタインジェクションのため
public class PayrollController {

    // フィールドインジェクション(@Autowired)を削除し、finalで宣言
    private final PayrollService payrollService;

    /**
     * 指定期間の給与計算を実行し、結果のDTOリストを返却するAPIメソッドです。
     *
     * 【機能】
     * フロントエンドから受け取った期間に基づき、給与計算サービスを呼び出します。
     *
     * 【注意事項】
     * クエリパラメータとして startDate と endDate が必須です。
     *
     * @param startDate 計算開始日 (YYYY-MM-DD形式)
     * @param endDate   計算終了日 (YYYY-MM-DD形式)
     * @return 計算結果のDTOリスト
     */
    @GetMapping("/calculate")
    public ResponseEntity<List<PayrollDto>> calculatePayroll(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<PayrollDto> payrolls = payrollService.calculatePayroll(startDate, endDate);

        return ResponseEntity.ok(payrolls);
    }
}