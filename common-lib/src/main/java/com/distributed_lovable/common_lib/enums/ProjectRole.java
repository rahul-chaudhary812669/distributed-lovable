package com.distributed_lovable.common_lib.enums;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;


@RequiredArgsConstructor
@Getter
public enum ProjectRole {

    VIEWER(Set.of(ProjectPermission.VIEW , ProjectPermission.VIEW_MEMBERS)),
    EDITOR( ProjectPermission.VIEW ,ProjectPermission.EDIT ,ProjectPermission.DELETE ,ProjectPermission.VIEW_MEMBERS),
    OWNER(Set.of(ProjectPermission.VIEW ,ProjectPermission.EDIT ,
                        ProjectPermission.DELETE ,ProjectPermission.MANAGE_MEMBERS, ProjectPermission.VIEW_MEMBERS));


    ProjectRole(ProjectPermission... permissions){
        this.permissions =  Set.of(permissions);
    }

    private final Set<ProjectPermission> permissions ;


}
