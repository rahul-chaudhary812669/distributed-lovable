package com.distributed_lovable.workspace_service.service.impl;


import com.distributed_lovable.common_lib.dto.UserDto;
import com.distributed_lovable.common_lib.error.ResourceNotFoundException;
import com.distributed_lovable.common_lib.security.AuthUtil;
import com.distributed_lovable.workspace_service.client.AccountClient;
import com.distributed_lovable.workspace_service.dto.member.InviteMemberRequest;
import com.distributed_lovable.workspace_service.dto.member.MemberResponse;
import com.distributed_lovable.workspace_service.dto.member.UpdateMemberRoleRequest;
import com.distributed_lovable.workspace_service.entity.Project;
import com.distributed_lovable.workspace_service.entity.ProjectMember;
import com.distributed_lovable.workspace_service.entity.ProjectMemberId;
import com.distributed_lovable.workspace_service.mapper.ProjectMemberMapper;
import com.distributed_lovable.workspace_service.repository.ProjectMemberRepository;
import com.distributed_lovable.workspace_service.repository.ProjectRepository;
import com.distributed_lovable.workspace_service.service.ProjectMemberService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults( level= AccessLevel.PRIVATE , makeFinal = true)
@Transactional
public class ProjectMemberServiceImpl implements ProjectMemberService {


    ProjectMemberRepository projectMemberRepository ;
    ProjectRepository projectRepository;
    ProjectMemberMapper projectMemberMapper;
    AuthUtil authUtil;
    AccountClient accountClient;


    @Override
    @PreAuthorize("@security.canViewMembers(#projectId)")
    public List<MemberResponse> getProjectMembers(Long projectId) {
             Long userId = authUtil.getCurrentUserId();

        Project project =   getAccessibleProjectById(projectId, userId);



        return   projectMemberRepository.findByIdProjectId(projectId).stream()
                 .map(projectMember -> {
                  UserDto user =  accountClient.getUserById(projectMember.getId().getUserId())
                                                          .orElseThrow(()-> new ResourceNotFoundException("user", projectMember.getId().getUserId().toString() ));
                 MemberResponse memberResponse =     projectMemberMapper.toProjectMemberResponseFromMember(projectMember);
                                       memberResponse.setName(user.name());
                                       memberResponse.setUsername(user.username());
                               return memberResponse ;
                         })
                 .toList();
    }




    @Override
    @PreAuthorize("@security.canManageMembers(#projectId)")
    public MemberResponse inviteMember(Long projectId, InviteMemberRequest request) {
        Long userId = authUtil.getCurrentUserId();

        Project project =   getAccessibleProjectById(projectId, userId);

       UserDto invitee =    accountClient.getUserByEmail(request.username())
               .orElseThrow(()-> new ResourceNotFoundException("user", request.username()));

        if(invitee.id().equals(userId)){
            throw new RuntimeException("not invite yourself");
        }

        ProjectMemberId projectMemberId = new ProjectMemberId( invitee.id() ,projectId );

        if(projectMemberRepository.existsById(projectMemberId)){
            throw new RuntimeException(" cannot invite once again ");
        }

     ProjectMember member =  ProjectMember.builder()
                .id(projectMemberId)
                .project(project)
                .projectRole(request.role())
                .invitedAt(Instant.now())
                .build();
      projectMemberRepository.save(member);

       MemberResponse memberResponse =   projectMemberMapper.toProjectMemberResponseFromMember(member);
       memberResponse.setName(invitee.name());
       memberResponse.setUsername(invitee.username());
       return memberResponse ;
    }


    @Override
    @PreAuthorize("@security.canManageMembers(#projectId)")
    public MemberResponse updateMemberRole(Long projectId, Long memberId, UpdateMemberRoleRequest request) {
        Long userId = authUtil.getCurrentUserId();
        Project project =   getAccessibleProjectById(projectId, memberId);


  //      ProjectMemberId projectMemberId = new ProjectMemberId(projectId , memberId);
        ProjectMember projectMember = projectMemberRepository.findByIdProjectIdAndIdUserId(projectId , memberId)
                                           .orElseThrow(()-> new ResourceNotFoundException("projectMember", "for"+projectId+"/"+"userId"));
        projectMember.setProjectRole(request.role());

         projectMemberRepository.save(projectMember);

        UserDto user =  accountClient.getUserById(memberId)
                .orElseThrow(()-> new ResourceNotFoundException("user", memberId.toString() ));
        MemberResponse memberResponse =     projectMemberMapper.toProjectMemberResponseFromMember(projectMember);
        memberResponse.setName(user.name());
        memberResponse.setUsername(user.username());
        return memberResponse ;



    }



    @Override
    @PreAuthorize("@security.canManageMembers(#projectId)")
    public void removeProjectMember(Long projectId, Long memberId) {
        Long userId = authUtil.getCurrentUserId();
        Project project =   getAccessibleProjectById(projectId, userId);


        ProjectMemberId projectMemberId = new ProjectMemberId(projectId , memberId);
        if(!projectMemberRepository.existsById(projectMemberId)){
            throw new RuntimeException(" member  not found in project ");
        }

           projectMemberRepository.deleteById(projectMemberId);

    }





    /// internal function

    public Project getAccessibleProjectById(Long projectId , Long userId){
        return  projectRepository.findAccessibleProjectById(projectId,userId).orElseThrow();
    }




}
