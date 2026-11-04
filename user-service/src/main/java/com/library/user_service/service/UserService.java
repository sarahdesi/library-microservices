package com.library.user_service.service;

import com.library.user_service.dtos.UserRequest;
import com.library.user_service.dtos.UserResponse;
import com.library.user_service.exception.BusinessException;
import com.library.user_service.exception.ResourceNotFoundException;
import com.library.user_service.model.User;
import com.library.user_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already registered");
        }
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setType(request.type());
        return UserResponse.from(userRepository.save(user));
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse findById(Long id) {
        return UserResponse.from(getUser(id));
    }

    public UserResponse update(Long id, UserRequest request) {
        User user = getUser(id);
        boolean emailChanged = !user.getEmail().equals(request.email());
        if (emailChanged && userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already registered");
        }
        user.setName(request.name());
        user.setEmail(request.email());
        user.setType(request.type());
        return UserResponse.from(userRepository.save(user));
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}