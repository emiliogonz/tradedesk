package com.example.tradedesk.service;

import com.example.tradedesk.domain.Role;
import com.example.tradedesk.domain.User;
import com.example.tradedesk.dto.UserResponse;
import com.example.tradedesk.exception.LastAdminException;
import com.example.tradedesk.exception.UserNotFoundException;
import com.example.tradedesk.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserAdminService {

    private final UserRepository userRepository;

    public UserAdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse changeRole(Long userId, Role newRole) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (newRole != Role.ADMIN) {
            // Lock every admin row so concurrent demotions are checked one at a time.
            List<User> admins = userRepository.findByRoleForUpdate(Role.ADMIN);
            boolean targetIsAdmin = admins.stream().anyMatch(a -> a.getId().equals(userId));
            if (targetIsAdmin && admins.size() == 1) {
                throw new LastAdminException();
            }
        }

        target.setRole(newRole);
        return new UserResponse(target.getId(), target.getEmail(), target.getRole().name());
    }
}