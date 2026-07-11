package com.distributed_lovable.workspace_service.mapper;



import com.distributed_lovable.workspace_service.dto.member.MemberResponse;
import com.distributed_lovable.workspace_service.entity.ProjectMember;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProjectMemberMapper {



    @Mapping( source="id.userId" , target="userId")
    MemberResponse toProjectMemberResponseFromMember(ProjectMember projectMember);



}


