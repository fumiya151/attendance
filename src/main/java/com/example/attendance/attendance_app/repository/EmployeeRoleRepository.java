package com.example.attendance.attendance_app.repository;

import com.example.attendance.attendance_app.model.EmployeeRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRoleRepository extends JpaRepository<EmployeeRole, Long> {
    /**
     * 従業員IDに基づいて、割り当てられた役割情報を全て取得する
     *
     * 【機能】
     * 指定された employeeId に関連付けられている全ての役割割当 (EmployeeRole) エンティティを取得します。
     *
     * 【注意事項】
     * 従業員が複数の役割を持つ場合、その役割の数だけレコードが返されます。
     *
     * @param employeeId 従業員ID
     * @return 役割割当エンティティのリスト
     */
    List<EmployeeRole> findByEmployeeId(String employeeId);

    /**
     * 指定された従業員IDに関連付けられた役割情報を全て削除します。
     *
     * 【機能】
     * 指定された employeeId に対応する EmployeeRole レコードをデータベースからすべて削除します。
     *
     * 【注意事項】
     * この操作はトランザクション内で実行されることが期待されます。
     *
     * @param employeeId 削除対象の従業員ID
     */
    void deleteByEmployeeId(String employeeId);
}