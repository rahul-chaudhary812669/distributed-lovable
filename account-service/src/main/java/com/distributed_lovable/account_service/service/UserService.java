package com.distributed_lovable.account_service.service;


import com.distributed_lovable.account_service.dto.auth.UserProfileResponse;

public interface UserService {

     UserProfileResponse getProfile(Long userId);
}
