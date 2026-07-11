package com.distributed_lovable.workspace_service.service.impl;


import com.distributed_lovable.common_lib.dto.FileNode;
import com.distributed_lovable.common_lib.dto.FileTreeDto;
import com.distributed_lovable.common_lib.error.ResourceNotFoundException;
import com.distributed_lovable.workspace_service.dto.project.FileContentResponse;
import com.distributed_lovable.workspace_service.entity.Project;
import com.distributed_lovable.workspace_service.entity.ProjectFile;
import com.distributed_lovable.workspace_service.mapper.ProjectFileMapper;
import com.distributed_lovable.workspace_service.repository.ProjectFileRepository;
import com.distributed_lovable.workspace_service.repository.ProjectRepository;
import com.distributed_lovable.workspace_service.service.ProjectFileService;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProjectFileServiceImpl implements ProjectFileService {

    private final ProjectRepository projectRepository;
    private final ProjectFileRepository projectFileRepository;
    private final MinioClient minioClient;
    private final ProjectFileMapper projectFileMapper;

    @Value("${minio.project-bucket}")
    private String projectBucket ;


    @Override
    public FileTreeDto getFileTree(Long projectId) {
         List<ProjectFile> fileTree =   projectFileRepository.findByProjectId( projectId);
          List<FileNode> files =   projectFileMapper.toListOfFileNode(fileTree);
          return new FileTreeDto(files);
    }



    @Override
    public String getFileContent(Long projectId, String path) {

           String objectKey = projectId+"/"+path ;
           try{

               InputStream is = minioClient.getObject(GetObjectArgs
                       .builder()
                               .bucket(projectBucket)
                               .object(objectKey)
                       .build());


              return  new String(is.readAllBytes(), StandardCharsets.UTF_8);

           }catch(Exception e){
               log.error("failed to read file {}/{}", projectId, path,e);
               throw new RuntimeException("failed to read file content",e);
           }

    }

    





    @Override
    @Transactional
    public void saveFile(Long projectId, String path, String content) {

      Project project =  projectRepository.findById(projectId)
                                      .orElseThrow(()-> new ResourceNotFoundException("project", projectId.toString()));

      String cleanPath = path.startsWith("/") ? path.substring(1) : path ;
      String objectKey = projectId+"/"+cleanPath;


        try {
          // saving content in minio
          byte[] contentBytes =   content.getBytes(StandardCharsets.UTF_8);
          InputStream inputStream = new ByteArrayInputStream(contentBytes);
           minioClient.putObject(
                PutObjectArgs
                        .builder()
                        .bucket(projectBucket)
                        .object(objectKey)
                        .stream(inputStream , contentBytes.length, -1)
                        .contentType(determineContentType(path))
                        .build()
        );

             // saving file metadata in database
            ProjectFile file =  projectFileRepository.findByProjectIdAndPath(projectId , cleanPath)
                    .orElseGet(()->ProjectFile
                            .builder()
                            .project(project)
                            .path(cleanPath)
                            .minioObjectKey(objectKey)
                           .createdAt(Instant.now())
                            .build());

            file.setUpdatedAt(Instant.now());
            projectFileRepository.save(file);

            log.info("file saved {}", objectKey);

        } catch (Exception e) {
            log.error("failed to saved file {}/{}/{}" , projectId , path , e.getMessage());
            throw new RuntimeException("file save failed" , e);
        }


    }




    /// utility methods
    private String determineContentType(String path){
        String type = URLConnection.guessContentTypeFromName(path);
        if(type!=null) return type ;
        if(path.endsWith(".jsx") || path.endsWith(".ts") || path.endsWith(".tsx") ) return "text/javascript";
        if(path.endsWith(".json"))  return "application/json" ;
        if(path.endsWith(".css")) return "text/css" ;

        return "text/plain";
    }





}
