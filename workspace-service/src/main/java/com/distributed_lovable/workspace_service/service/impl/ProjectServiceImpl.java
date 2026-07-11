package com.distributed_lovable.workspace_service.service.impl;


import com.distributed_lovable.common_lib.dto.PlanDto;
import com.distributed_lovable.common_lib.dto.UserDto;
import com.distributed_lovable.common_lib.enums.ProjectPermission;
import com.distributed_lovable.common_lib.enums.ProjectRole;
import com.distributed_lovable.common_lib.error.BadRequestException;
import com.distributed_lovable.common_lib.error.ResourceNotFoundException;
import com.distributed_lovable.common_lib.security.AuthUtil;
import com.distributed_lovable.workspace_service.client.AccountClient;
import com.distributed_lovable.workspace_service.dto.project.ProjectRequest;
import com.distributed_lovable.workspace_service.dto.project.ProjectResponse;
import com.distributed_lovable.workspace_service.dto.project.ProjectSummaryResponse;
import com.distributed_lovable.workspace_service.entity.Project;
import com.distributed_lovable.workspace_service.entity.ProjectMember;
import com.distributed_lovable.workspace_service.entity.ProjectMemberId;
import com.distributed_lovable.workspace_service.mapper.ProjectMapper;
import com.distributed_lovable.workspace_service.repository.ProjectMemberRepository;
import com.distributed_lovable.workspace_service.repository.ProjectRepository;
import com.distributed_lovable.workspace_service.security.SecurityExpressions;
import com.distributed_lovable.workspace_service.service.ProjectService;
import com.distributed_lovable.workspace_service.service.ProjectTemplateService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Transactional
@Slf4j
public class ProjectServiceImpl implements ProjectService {


    ProjectRepository projectRepository;
    ProjectMemberRepository projectMemberRepository;
    ProjectMapper projectMapper;
    AuthUtil authUtil;
    ProjectTemplateService projectTemplateService;
    AccountClient accountClient;
    SecurityExpressions securityExpressions ;

    int PROJECTS_ALLOW_TO_FREE_USERS = 1 ;


    @Override
    public ProjectResponse createProject(ProjectRequest request) {

        if(!canCreateNewProject()){
            throw new BadRequestException(" user cannot create a new project with current plan , upgrade plan now");
        }

        Long ownerUserId = authUtil.getCurrentUserId();

        Project project = Project.builder()
                .name(request.name())
                .isPublic(false)
                .build();

        project  = projectRepository.save(project);

        ProjectMemberId projectMemberId = new ProjectMemberId(ownerUserId , project.getId());
        ProjectMember projectMember = ProjectMember.builder()
                .id(projectMemberId)
                .project(project)
                .projectRole(ProjectRole.OWNER)
                .invitedAt(Instant.now())
                .acceptedAt(Instant.now())
                .build();

        projectMemberRepository.save(projectMember);

        log.info("initializing template for project {}", project.getId());
        projectTemplateService.initializeProjectFromTemplate(project.getId());

        return  projectMapper.toProjectResponse(project);

    }


    @Override
    public List<ProjectSummaryResponse> getUserProjects() {
        Long userId = authUtil.getCurrentUserId();

      return   projectRepository.findAllAccessibleByUser(userId)
              .stream()
              .map(projectWithRole ->
                      projectMapper.toProjectSummaryResponse(projectWithRole.getProject(), projectWithRole.getRole()))
              .toList();
    }


    @Override
    @PreAuthorize("@security.canViewProject(#projectId)")
    public ProjectSummaryResponse getUserProjectById(Long projectId) {
        Long userId = authUtil.getCurrentUserId();

    ProjectRepository.ProjectWithRole projectWithRole =   projectRepository.findAccessibleProjectByIdWithRole(projectId , userId)
                                                         .orElseThrow(()-> new BadRequestException("project not found"));
        return  projectMapper.toProjectSummaryResponse(projectWithRole.getProject() , projectWithRole.getRole());
    }



    @Override
    @PreAuthorize("@security.canEditProject(#projectId)")
    public ProjectResponse updateProject(Long projectId, ProjectRequest request) {
        Long userId = authUtil.getCurrentUserId();

             Project project =  getAccessibleProjectById( projectId,userId);

             project.setName(request.name());
             project =  projectRepository.save(project);

        return  projectMapper.toProjectResponse(project);
    }


    @Override
   @PreAuthorize("@security.canDeleteProject(#projectId)")
    public void softDelete(Long projectId) {
        Long userId = authUtil.getCurrentUserId();

           Project project = getAccessibleProjectById(projectId,userId);

           project.setDeletedAt(Instant.now());
           projectRepository.save(project);
    }


    @Override
    public boolean hasPermission(Long projectId, ProjectPermission permission) {
        return  securityExpressions.hasPermission(projectId , permission);
    }


    /// internal function

    public Project getAccessibleProjectById(Long projectId , Long userId){
      return  projectRepository.findAccessibleProjectById(projectId,userId)
               .orElseThrow(()-> new ResourceNotFoundException("project", projectId.toString()));
    }


    public boolean canCreateNewProject() {

        Long userId =  authUtil.getCurrentUserId();

        if(userId==null){
            return false ;
        }



       PlanDto plan  =  accountClient.getCurrentSubscribedPlanByUser();

        if(plan==null){
            int countOfOwnedProjects = projectMemberRepository.countProjectOwnedByUser(userId);
            if(countOfOwnedProjects < PROJECTS_ALLOW_TO_FREE_USERS){
                return true ;
            }else{
                return false ;
            }
        }

        int maxAllowed =  plan.maxProjects();
        int countOfOwnedProjects = projectMemberRepository.countProjectOwnedByUser(userId);
        return countOfOwnedProjects < maxAllowed ;
    }





}
