package com.distributed_lovable.workspace_service.entity;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
public class ProcessedEvent{

    @Id
  private  String sagaId;
  private  LocalDateTime processedAt ;



}
