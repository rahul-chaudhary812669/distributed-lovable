package com.distributed_lovable.workspace_service.repository;

import com.distributed_lovable.common_lib.enums.ProjectRole;
import com.distributed_lovable.workspace_service.entity.ProjectMember;
import com.distributed_lovable.workspace_service.entity.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {


          List<ProjectMember> findByIdProjectId(Long projectId);

             Optional<ProjectMember>  findByIdProjectIdAndIdUserId(Long projectId , Long userId);

          @Query("""
                 SELECT pm.projectRole FROM ProjectMember pm
                 WHERE pm.id.userId = :userId
                 AND pm.id.projectId = :projectId
                 """)
          Optional<ProjectRole> findRoleByProjectIdAndUserId(@Param("projectId") Long projectId ,
                                                             @Param("userId") Long userId);

          @Query("""
                 SELECT COUNT(pm) FROM ProjectMember pm
                 WHERE pm.id.userId = :userId AND pm.projectRole = 'OWNER'
                 """)
          int countProjectOwnedByUser(@Param("userId") Long userId);



}
