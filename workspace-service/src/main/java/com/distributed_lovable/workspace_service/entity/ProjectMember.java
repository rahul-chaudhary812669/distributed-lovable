package com.distributed_lovable.workspace_service.entity;

import com.distributed_lovable.common_lib.enums.ProjectRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table( name="projectMembers")
public class ProjectMember {

    @EmbeddedId
    ProjectMemberId  id;

    @ManyToOne
    @MapsId("projectId")
    Project project;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ProjectRole projectRole;


    Instant invitedAt;


    Instant acceptedAt;


}
