package com.capsule.user;

import com.capsule.user.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(Authentication auth) {
        var userId = (UUID) auth.getPrincipal();
        var user = userService.findById(userId);
        return ResponseEntity.ok(new UserResponse(user.getId(), user.getEmail(), user.getTier()));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@RequestBody UpdateUserRequest req,
                                                  Authentication auth) {
        var userId = (UUID) auth.getPrincipal();
        var user = userService.updateEmail(userId, req.email());
        return ResponseEntity.ok(new UserResponse(user.getId(), user.getEmail(), user.getTier()));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(Authentication auth) {
        var userId = (UUID) auth.getPrincipal();
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
}
