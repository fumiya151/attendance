package com.example.attendance.attendance_app.repository;

import com.example.attendance.attendance_app.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * 権限レベル（roleLevel）が指定された値以上の全ての役割を取得します。
     *
     * 【機能】
     * roleLevel が指定された値以上の全ての Role エンティティを検索し、リストとして返却します。
     *
     * 【注意事項】
     * これは、特定の権限レベル以上のユーザー（例：管理者）を識別するために使用されます。
     *
     * @param roleLevel 最小の権限レベル
     * @return 役割エンティティのリスト
     */
    List<Role> findByRoleLevelGreaterThanEqual(Integer roleLevel);

    /**
     * 役割コード（roleCode）を指定して役割を取得します。
     *
     * 【機能】
     * 一意の roleCode に一致する Role エンティティを検索します。
     *
     * 【注意事項】
     * roleCode は一意の識別子として使用されることを想定しています。
     *
     * @param roleCode 役割コード
     * @return 役割エンティティ（Optional）
     */
    Optional<Role> findByRoleCode(String roleCode);
}