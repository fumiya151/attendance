package com.example.attendance.attendance_app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.attendance.attendance_app.model.Employee;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {

    /**
     * 氏名または従業員IDが、指定された文字列を部分一致で含む従業員を検索します。
     *
     * 【機能】
     * 従業員の Name (氏名) または EmployeeId (従業員ID/コード)
     * のいずれかが、指定された検索キーワードを大文字・小文字を区別せず（IgnoreCase）に含むレコードをすべて取得します。
     *
     * 【注意事項】
     * 検索キーワードは、氏名検索用と従業員ID検索用にそれぞれ1つずつ（計2つ）必要です。通常、同じ値を name と code に渡して使用します。
     *
     * @param name 氏名で検索するためのキーワード
     * @param code 従業員IDで検索するためのキーワード
     * @return 条件に一致する Employeeエンティティのリスト
     */
    List<Employee> findByNameContainingIgnoreCaseOrEmployeeIdContainingIgnoreCase(String name, String code);
}