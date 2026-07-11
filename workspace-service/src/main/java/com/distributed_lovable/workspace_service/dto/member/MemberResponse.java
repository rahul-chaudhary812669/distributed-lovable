package com.distributed_lovable.workspace_service.dto.member;



import com.distributed_lovable.common_lib.enums.ProjectRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
 @AllArgsConstructor
 @NoArgsConstructor
 @Getter
 @Setter
public class MemberResponse
{

  private  Long userId  ;
   private  String name  ;
  private  String username ;
  private  ProjectRole projectRole ;
  private  Instant invitedAt ;




}
