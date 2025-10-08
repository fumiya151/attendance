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
     * これは、特定の権限レベル以上のユーザーを識別するために使用されます。
     *
     * @param roleLevel 最小の権限レベル
     * @return 役割エンティティのリスト
     */
    List<Role> findByRoleLevelGreaterThanEqual(Integer roleLevel);

    /**
     * 役割コード（roleCode）を指定して役割を取得します。
     *
     * @param roleCode 役割コード
     * @return 役割エンティティ（Optional）
     */
    Optional<Role> findByRoleCode(String roleCode);
}
