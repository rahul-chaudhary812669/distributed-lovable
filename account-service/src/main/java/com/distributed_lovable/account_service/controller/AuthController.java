package com.distributed_lovable.account_service.controller;


import com.distributed_lovable.account_service.dto.auth.AuthResponse;
import com.distributed_lovable.account_service.dto.auth.LoginRequest;
import com.distributed_lovable.account_service.dto.auth.SignupRequest;
import com.distributed_lovable.account_service.service.AuthService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthController {


      AuthService authService;



     @PostMapping("/signup")
     public ResponseEntity<AuthResponse> signup(@RequestBody SignupRequest request){
            return ResponseEntity.ok(authService.signup(request));
     }


     @PostMapping("/login")
     public ResponseEntity<AuthResponse> login( @RequestBody LoginRequest request){
           return ResponseEntity.ok(authService.login(request));
     }


//    @GetMapping("/me")
//    public ResponseEntity<UserProfileResponse> getProfile(){
//        Long userId = 1L;
//        return ResponseEntity.ok(userService.getProfile(userId));
//    }



}


