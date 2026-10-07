package com.example.tradedesk.controller;

import com.example.tradedesk.dto.ChangeRoleRequest;
import com.example.tradedesk.dto.UserResponse;
import com.example.tradedesk.service.UserAdminService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final UserAdminService userAdminService;

    public AdminUserController(UserAdminService userAdminService){
        this.userAdminService = userAdminService;
    }

    @PutMapping("/{userId}/role")
    public UserResponse changeRole(@PathVariable Long userId,
                                   @Valid @RequestBody ChangeRoleRequest request){
        return userAdminService.changeRole(userId, request.role());
    }
}
