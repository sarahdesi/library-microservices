package com.library.user_service.service;

import com.library.user_service.dtos.UserRequest;
import com.library.user_service.dtos.UserResponse;
import com.library.user_service.dtos.UserUpdateRequest;
import com.library.user_service.exception.BusinessException;
import com.library.user_service.exception.ConflictException;
import com.library.user_service.exception.ResourceNotFoundException;
import com.library.user_service.model.Role;
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
        if (userRepository.existsByCpf(request.cpf())) {
            throw new ConflictException("CPF already registered");
        }
        // Membro do balcao: sem senha, ate ele mesmo ativar o acesso pelo cadastro publico
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setType(request.type());
        user.setCpf(request.cpf());
        user.setRole(Role.MEMBER);
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

    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = getUser(id);
        boolean emailChanged = !user.getEmail().equals(request.email());
        if (emailChanged && userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already registered");
        }
        boolean cpfInformed = request.cpf() != null && !request.cpf().isBlank();
        if (cpfInformed && !request.cpf().equals(user.getCpf()) && userRepository.existsByCpf(request.cpf())) {
            throw new ConflictException("CPF already registered");
        }
        user.setName(request.name());
        user.setEmail(request.email());
        if (cpfInformed) {
            user.setCpf(request.cpf());
        }
        // A bibliotecaria nao tem tipo (nao toma emprestimos)
        if (user.getRole() != Role.LIBRARIAN) {
            user.setType(request.type());
        }
        return UserResponse.from(userRepository.save(user));
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}