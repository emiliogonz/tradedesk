package com.example.tradedesk.dto;

import com.example.tradedesk.domain.Role;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequest(@NotNull  Role role) {
}
