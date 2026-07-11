package com.distributed_lovable.workspace_service.security;


import com.distributed_lovable.common_lib.enums.ProjectPermission;
import com.distributed_lovable.common_lib.security.AuthUtil;
import com.distributed_lovable.workspace_service.repository.ProjectMemberRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;


@Component("security")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE , makeFinal = true)
public class SecurityExpressions {

    ProjectMemberRepository projectMemberRepository;
    AuthUtil authUtil ;


   public Boolean hasPermission(Long projectId , ProjectPermission projectPermission){
        long userId = authUtil.getCurrentUserId();

        return   projectMemberRepository.findRoleByProjectIdAndUserId(projectId,userId)
                .map(role -> role.getPermissions().contains(projectPermission) )
                .orElse(false);
    }


    public boolean canViewProject(Long projectId){
        return hasPermission(projectId , ProjectPermission.VIEW);
    }


    public boolean canEditProject(Long projectId){
        return hasPermission(projectId , ProjectPermission.EDIT);
    }


    public boolean canDeleteProject(Long projectId){
        return hasPermission(projectId , ProjectPermission.DELETE);
    }

    public boolean canViewMembers(Long projectId){
        return hasPermission(projectId , ProjectPermission.VIEW_MEMBERS);
    }

    public boolean canManageMembers(Long projectId){
        return hasPermission(projectId , ProjectPermission.MANAGE_MEMBERS);
    }









}
