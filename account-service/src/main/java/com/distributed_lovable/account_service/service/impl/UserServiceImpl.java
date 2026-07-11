package com.distributed_lovable.account_service.service.impl;


import com.distributed_lovable.account_service.entity.User;
import com.distributed_lovable.account_service.repository.UserRepository;
import com.distributed_lovable.common_lib.error.ResourceNotFoundException;
import com.distributed_lovable.common_lib.security.JwtUserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserDetailsService {

     private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

     User user =  userRepository.findByUsername(username)
                              .orElseThrow(()-> new ResourceNotFoundException("user", username));
        return new JwtUserPrincipal(user.getId(),user.getName(),
                                    user.getUsername(),user.getPassword()
                                      ,new ArrayList<>());
    }





}

