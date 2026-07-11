package com.distributed_lovable.workspace_service.controller;

import com.distributed_lovable.common_lib.dto.FileTreeDto;
import com.distributed_lovable.workspace_service.dto.project.FileContentResponse;
import com.distributed_lovable.workspace_service.service.ProjectFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/projects/{projectId}/files")
@RequiredArgsConstructor
public class FileController {

       private final ProjectFileService fileService;

       @GetMapping
       public ResponseEntity<FileTreeDto> getFileTree(@PathVariable Long projectId){

           return  ResponseEntity.ok(fileService.getFileTree(projectId ));
       }


       @GetMapping("/content")   // src/hooks/get-user-hooks.jsx
       public ResponseEntity<String> getFile(
               @RequestParam String path
               , @PathVariable Long projectId){

         return ResponseEntity.ok(fileService.getFileContent(projectId , path ));
       }



    




}
