package com.example.attendance.attendance_app.service;

import com.example.attendance.attendance_app.model.DailyAttendanceSummary;
import com.example.attendance.attendance_app.model.Employee;
import com.example.attendance.attendance_app.repository.DailyAttendanceSummaryRepository;
import com.example.attendance.attendance_app.repository.EmployeeRepository;
import com.example.attendance.attendance_app.dto.DailyAttendanceSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DailyAttendanceSummaryService {

    private final DailyAttendanceSummaryRepository summaryRepository;
    private final EmployeeRepository employeeRepository;

    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_PENDING = "PENDING";
    private static final ZoneId JST_ZONE = ZoneId.of("Asia/Tokyo");

    /**
     * 全期間の勤怠サマリーを従業員名情報と承認ステータス付きで取得します。
     *
     * @return DailyAttendanceSummaryDtoのリスト
     */
    public List<DailyAttendanceSummaryDto> getAllSummariesWithEmployeeInfo() {
        // 1. 全従業員情報を取得し、IDをキーとしたマップに変換 (高速検索のため)
        Map<String, String> employeeNamesById = employeeRepository.findAll().stream()
                .collect(Collectors.toMap(Employee::getEmployeeId, Employee::getName));

        // 2. 全ての勤怠サマリーを取得
        List<DailyAttendanceSummary> allSummaries = summaryRepository.findAll();

        // 3. サマリーと従業員名を結合し、DTOに変換
        return allSummaries.stream()
                .map(summary -> {
                    DailyAttendanceSummaryDto dto = new DailyAttendanceSummaryDto();

                    // 基本情報
                    dto.setId(summary.getId());
                    dto.setEmployeeId(summary.getEmployeeId());
                    dto.setWorkDate(summary.getWorkDate());

                    // 従業員名をマップから取得
                    dto.setEmployeeName(employeeNamesById.getOrDefault(summary.getEmployeeId(), "不明な従業員"));

                    // 勤務情報
                    dto.setActualInTime(summary.getActualInTime());
                    dto.setActualOutTime(summary.getActualOutTime());
                    dto.setTotalBreakMinutes(summary.getTotalBreakMinutes());
                    dto.setTotalWorkMinutes(summary.getTotalWorkMinutes());

                    // ステータス情報
                    dto.setLogStatus("集計済");
                    dto.setApprovalStatus(summary.getStatus());

                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 指定された勤怠サマリーIDのリストの中から、さらに指定期間内にあるものだけを「承認済み」に更新します。
     * (検索結果と期間による二重チェック、単体承認時は期間チェックをスキップ)
     *
     * @param summaryIds 承認対象のDailyAttendanceSummaryのIDリスト
     * @param startDate  チェック対象期間開始日
     * @param endDate    チェック対象期間終了日
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

                // ★ 修正された期間チェックロジック ★
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
}