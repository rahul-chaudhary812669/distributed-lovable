package com.distributed_lovable.common_lib.dto;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;


public record UserDto(
        Long id ,
        String username,
        String name
)  {

}
