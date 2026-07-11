package com.distributed_lovable.workspace_service.service.impl;

import com.distributed_lovable.common_lib.error.ResourceNotFoundException;
import com.distributed_lovable.workspace_service.entity.Project;
import com.distributed_lovable.workspace_service.entity.ProjectFile;
import com.distributed_lovable.workspace_service.repository.ProjectFileRepository;
import com.distributed_lovable.workspace_service.repository.ProjectRepository;
import com.distributed_lovable.workspace_service.service.ProjectTemplateService;
import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@RequiredArgsConstructor
@Slf4j
@Service
public class ProjectTemplateServiceImpl implements ProjectTemplateService {

    private final MinioClient minioClient;
    private final ProjectFileRepository projectFileRepository;
    private final ProjectRepository projectRepository;

    private static final String TEMPLATE_BUCKET = "starter-project" ;
    private static final String TARGET_BUCKET = "projects" ;
    private static final String TEMPLATE_NAME = "react-vite-tailwind-daisyui-starter" ;



    @Override
    public void initializeProjectFromTemplate(Long projectId) {
        log.info("fetching project {}", projectId);
        Project project = projectRepository.findById(projectId)
                                                   .orElseThrow(()-> new ResourceNotFoundException("project",projectId.toString()));

        try{
            log.info("getting objects from minio");
            Iterable<Result<Item>> results =  minioClient.listObjects(
                    ListObjectsArgs
                            .builder()
                            .bucket(TEMPLATE_BUCKET)
                            .prefix(TEMPLATE_NAME+"/")
                            .recursive(true)
                            .build()
            );

            log.info("list that will contains project file");
            List<ProjectFile> fileToSave =  new ArrayList<>();  // for metadata in db

            for(Result<Item> result: results){
                Item item =  result.get();
                String sourceKey =  item.objectName();

                log.info("cleaning the path");
                String cleanPath = sourceKey.replaceFirst(TEMPLATE_NAME+"/","");
                String destkey = projectId+"/"+cleanPath;

                log.info("copying objects from template bucket to projects bucket");
                minioClient.copyObject(
                        CopyObjectArgs
                                .builder()
                                .bucket(TARGET_BUCKET)
                                .object(destkey)
                                .source(
                                        CopySource
                                                .builder()
                                                .bucket(TEMPLATE_BUCKET)
                                                .object(sourceKey)
                                                .build()
                                )
                                .build()
                );

                log.info("saving project file");
                ProjectFile projectFile = ProjectFile
                        .builder()
                        .project(project)
                        .path(cleanPath)
                        .minioObjectKey(destkey)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();

                fileToSave.add(projectFile);
            }
            log.info("saving file to database");
            log.info("Saved {} files", fileToSave.size());
            projectFileRepository.saveAll(fileToSave);

        }catch (Exception e){
            log.error("Template initialization failed", e);
           throw new RuntimeException("failed to initialize project from template" , e);

        }






    }


}
