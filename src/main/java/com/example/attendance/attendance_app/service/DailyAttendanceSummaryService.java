package com.example.attendance.attendance_app.service;

import com.example.attendance.attendance_app.model.DailyAttendanceSummary;
import com.example.attendance.attendance_app.model.Employee;
import com.example.attendance.attendance_app.repository.DailyAttendanceSummaryRepository;
import com.example.attendance.attendance_app.repository.EmployeeRepository;
import com.example.attendance.attendance_app.dto.DailyAttendanceSummaryDto;
import com.example.attendance.attendance_app.dto.MonthlySummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyAttendanceSummaryService {

    private final DailyAttendanceSummaryRepository summaryRepository;
    private final EmployeeRepository employeeRepository;
    // PDF生成の責務は AttendancePdfService が持つため、ここでのインジェクションは削除します。
    // private final AttendancePdfService pdfService; // 削除

    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_PENDING = "PENDING";
    private static final ZoneId JST_ZONE = ZoneId.of("Asia/Tokyo");

    /**
     * 指定された月度の全従業員の勤怠サマリーを集計するメソッドです。
     *
     * 【機能】
     * 指定された月内の全従業員の総労働時間と平均残業時間を計算します。
     * 標準労働時間は8時間/日として計算します。
     *
     * 【注意事項】
     * 計算はサマリーテーブルのデータに基づいて行われます。平均残業時間は、その月に勤怠データが存在した従業員の数で割って算出されます。
     *
     * @param yearMonth 集計対象の年月 (YearMonthオブジェクト)
     * @return 月間集計結果のDTO (MonthlySummaryDto)
     */
    public MonthlySummaryDto getMonthlySummary(YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<DailyAttendanceSummary> summaries = summaryRepository.findByWorkDateBetween(startDate, endDate);

        long totalMinutes = summaries.stream().mapToLong(DailyAttendanceSummary::getTotalWorkMinutes).sum();
        double totalHours = totalMinutes / 60.0;

        // 休憩時間が引かれた総労働時間から、標準労働時間（8時間）を超過した分を残業時間として計算
        long totalOvertimeMinutes = summaries.stream().mapToLong(summary -> {
            long standardWorkMinutes = 8 * 60;
            long overtime = summary.getTotalWorkMinutes() - standardWorkMinutes;
            return Math.max(overtime, 0);
        }).sum();

        long numberOfEmployees = summaries.stream().map(DailyAttendanceSummary::getEmployeeId).distinct().count();

        double averageOvertimeHours = 0;
        if (numberOfEmployees > 0) {
            averageOvertimeHours = (totalOvertimeMinutes / 60.0) / numberOfEmployees;
        }

        return new MonthlySummaryDto(totalHours, averageOvertimeHours);
    }

    /**
     * 全期間の勤怠サマリーを従業員名情報と承認ステータス付きで取得します。
     *
     * 【機能】
     * 全ての勤怠サマリーレコードを取得し、従業員名と結合したDTOリストを返却します。
     *
     * 【注意事項】
     * リストは workDate の降順（新しい日付が先）でソートされます。
     *
     * @return DailyAttendanceSummaryDtoのリスト
     */
    public List<DailyAttendanceSummaryDto> getAllSummariesWithEmployeeInfo() {
        // 1. 全従業員情報を取得し、IDをキーとしたマップに変換
        Map<String, String> employeeNamesById = employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getEmployeeId, Employee::getName));

        // 2. 全ての勤怠サマリーを取得
        List<DailyAttendanceSummary> allSummaries = summaryRepository.findAll();

        // 3. サマリーを日付降順でソートし、従業員名と結合してDTOに変換
        return allSummaries.stream()
                .sorted(Comparator.comparing(DailyAttendanceSummary::getWorkDate).reversed())
                .map(summary -> convertToDailySummaryDto(summary, employeeNamesById))
                .collect(Collectors.toList());
    }

    /**
     * 指定された従業員と月度の勤怠サマリーを取得します。
     *
     * 【機能】
     * 指定された従業員IDと年月に基づいて、その月の勤怠サマリーデータを取得します。
     *
     * 【注意事項】
     * 取得されたリストは、勤務表形式に合わせるため、workDate の昇順（古い日付が先）でソートされます。
     *
     * @param employeeId 従業員ID
     * @param yearMonth  対象年月
     * @return DailyAttendanceSummaryDtoのリスト（日付昇順でソート済み）
     */
    public List<DailyAttendanceSummaryDto> findSummariesByEmployeeAndMonth(String employeeId, YearMonth yearMonth) {
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 従業員名マップの準備
        Map<String, String> employeeNamesById = employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getEmployeeId, Employee::getName));

        // Repositoryから従業員IDと期間でサマリーを検索
        // NOTE: findByEmployeeIdAndWorkDateBetween が Repositoryに存在することを前提とします
        List<DailyAttendanceSummary> summaries = summaryRepository.findByEmployeeIdAndWorkDateBetween(
                employeeId, startDate, endDate);

        // DTOに変換し、日付でソート（勤務表形式に合わせ、昇順）
        return summaries.stream()
                .sorted(Comparator.comparing(DailyAttendanceSummary::getWorkDate))
                .map(summary -> convertToDailySummaryDto(summary, employeeNamesById))
                .collect(Collectors.toList());
    }

    /**
     * 指定された勤怠サマリーを「承認済み」に更新するメソッドです。
     *
     * 【機能】
     * IDリストに基づいてサマリーを取得し、期間チェック（単体承認時はスキップ）とステータスチェック（PENDINGのみ）を行った上で、APPROVEDに更新します。
     *
     * 【注意事項】
     * このメソッドはトランザクション内で実行されます。承認操作を行うapproverIdが存在しない場合はRuntimeExceptionがスローされます。
     *
     * @param summaryIds 承認対象のDailyAttendanceSummaryのIDリスト
     * @param startDate  チェック対象期間開始日（単体承認時は無視される）
     * @param endDate    チェック対象期間終了日（単体承認時は無視される）
     * @param approverId 承認操作を行った管理者ID
     * @return 承認されたレコード数
     */
    @Transactional
    public int approveSummariesByIds(
            List<Long> summaryIds,
            LocalDate startDate,
            LocalDate endDate,
            String approverId) {

        if (summaryIds == null || summaryIds.isEmpty()) {
            return 0;
        }

        employeeRepository.findById(approverId)
                .orElseThrow(() -> new RuntimeException("承認操作を行う従業員IDが見つかりません: " + approverId));

        OffsetDateTime approvalTime = OffsetDateTime.now(JST_ZONE);

        // 単体承認時は期間チェックをスキップするかどうかを決定
        final boolean skipDateCheck = summaryIds.size() == 1;

        // 1. IDリストに基づいて対象のサマリーを取得
        List<DailyAttendanceSummary> summariesToApprove = summaryRepository.findAllById(summaryIds);

        // 2. 更新が必要な（PENDING状態 AND (単体承認OR期間内)）のサマリーのみを抽出
        List<DailyAttendanceSummary> updatedSummaries = summariesToApprove.stream()
                // PENDING状態のものに絞る
                .filter(summary -> STATUS_PENDING.equals(summary.getStatus()))

                // ★ 修正された期間チェックロジック（単体承認時は日付チェックを無視） ★
                .filter(summary -> skipDateCheck ||
                        (!summary.getWorkDate().isBefore(startDate) && !summary.getWorkDate().isAfter(endDate)))

                .peek(summary -> {
                    // APPROVED に更新
                    summary.setStatus(STATUS_APPROVED);
                    summary.setApprovedById(approverId);
                    summary.setApprovedAt(approvalTime);
                    summary.setUpdatedById(approverId);
                })
                .collect(Collectors.toList());

        // 3. 一括で保存/更新を実行
        summaryRepository.saveAll(updatedSummaries);

        // 4. 更新件数を返す
        return updatedSummaries.size();
    }

    /**
     * 最新の日次勤怠サマリー10件を、新しい順（ID降順）で取得し、氏名を付与してDTOに変換します。
     *
     * 【機能】
     * リポジトリから最新の10件の勤怠サマリーエンティティを取得し、全従業員情報と結合してDTOリストとして返却します。
     *
     * 【注意事項】
     * 従業員名結合のため、全従業員を取得してからマッピングを行います。リストはID降順（最新順）です。
     *
     * @return 最新10件の DailyAttendanceSummaryDto リスト
     */
    public List<DailyAttendanceSummaryDto> findLatest10DailySummaries() {
        // 1. Repositoryからエンティティのリストを取得 (最新10件)
        List<DailyAttendanceSummary> summaries = summaryRepository.findTop10ByOrderByIdDesc();

        // 2. 従業員IDと氏名のマッピングを効率化するために全従業員を取得
        Map<String, String> employeeNameMap = employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getEmployeeId, Employee::getName,
                        (existing, replacement) -> existing)); // マッピングの競合回避を追加

        // 3. エンティティをDTOに変換
        return summaries.stream()
                .map(summary -> convertToDailySummaryDto(summary, employeeNameMap))
                .collect(Collectors.toList());
    }

    /**
     * DailyAttendanceSummaryエンティティをDailyAttendanceSummaryDtoに変換するヘルパーメソッド。
     *
     * 【機能】
     * 勤怠サマリーエンティティの内容をDTOにコピーし、別途取得したマップから従業員名を付与します。
     *
     * 【注意事項】
     * 従業員名マップに該当IDがない場合、「不明な従業員」というデフォルト値が設定されます。
     *
     * @param summary         変換元の勤怠サマリーエンティティ
     * @param employeeNameMap 従業員IDと氏名をマッピングしたマップ
     * @return 従業員名が付与された DailyAttendanceSummaryDto
     */
    private DailyAttendanceSummaryDto convertToDailySummaryDto(
            DailyAttendanceSummary summary, Map<String, String> employeeNameMap) {

        DailyAttendanceSummaryDto dto = new DailyAttendanceSummaryDto();
        dto.setId(summary.getId());
        dto.setEmployeeId(summary.getEmployeeId());

        String employeeName = employeeNameMap.getOrDefault(summary.getEmployeeId(), "不明な従業員");
        dto.setEmployeeName(employeeName);

        dto.setWorkDate(summary.getWorkDate());
        dto.setActualInTime(summary.getActualInTime());
        dto.setActualOutTime(summary.getActualOutTime());
        dto.setTotalBreakMinutes(summary.getTotalBreakMinutes());
        dto.setTotalWorkMinutes(summary.getTotalWorkMinutes());

        dto.setApprovalStatus(summary.getStatus());

        return dto;
    }
}