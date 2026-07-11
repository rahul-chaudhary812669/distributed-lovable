package com.distributed_lovable.workspace_service.consumer;

import com.distributed_lovable.common_lib.event.FileStoreRequestEvent;
import com.distributed_lovable.common_lib.event.FileStoreResponseEvent;
import com.distributed_lovable.workspace_service.entity.ProcessedEvent;
import com.distributed_lovable.workspace_service.repository.ProcessedEventRepository;
import com.distributed_lovable.workspace_service.service.ProjectFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageConsumer {

    private final ProjectFileService  projectFileService;
    private final ProcessedEventRepository processedEventRepository;
    private final KafkaTemplate<String,Object> kafkaTemplate ;

       @KafkaListener(topics="file-storage-request-event", groupId = "workspace-group")
       @Transactional
       public void fileConsumer(FileStoreRequestEvent event){

           log.info("consuming file-storage-request-event" );

           // Idempotency check
           if(processedEventRepository.existsById(event.sagaId())){
               log.info("Duplicate Saga Detected: {}, Resending previous Acknowledgement.", event.sagaId());
               sendResponse(event ,  true  ,null);
               return ;
           }

           try{
               log.info("saving file: {}", event.filePath());
               projectFileService.saveFile(event.projectId(), event.filePath(),  event.content());
               sendResponse(event ,  true  ,null);
               processedEventRepository.save( new ProcessedEvent(event.sagaId() , LocalDateTime.now()));

           }catch(Exception e){
               log.error("saving file error: {}",e.getMessage());
               sendResponse(event ,false  ,e.getMessage());
           }


         }



       private void sendResponse(FileStoreRequestEvent event , boolean success , String error){
      FileStoreResponseEvent response =  FileStoreResponseEvent
                   .builder()
                   .sagaId(event.sagaId())
                   .projectId(event.projectId())
                   .success(success)
                   .errorMessage(error)
                   .build();
           log.info(" event , file-storage-response sent to inventory service ");
           kafkaTemplate.send("file-store-response" , response);
       }





}
