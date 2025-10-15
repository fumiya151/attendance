package com.example.attendance.attendance_app.repository;

import com.example.attendance.attendance_app.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface LoginRepository extends JpaRepository<Employee, String> {
    /**
     * 指定された従業員IDに基づいて従業員情報を取得します。
     *
     * 【機能】
     * ログイン時や認証時に、入力された従業員IDに一致する Employee エンティティを検索します。
     *
     * 【注意事項】
     * 従業員IDは一意であるため、結果は Optional<Employee> として返却されます。
     *
     * @param employeeId 検索対象の従業員ID
     * @return 該当する従業員エンティティ（Optional）
     */
    Optional<Employee> findByEmployeeId(String employeeId);
}