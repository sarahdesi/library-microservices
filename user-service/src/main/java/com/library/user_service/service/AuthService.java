package com.library.user_service.service;

import com.library.user_service.dtos.LoginRequest;
import com.library.user_service.dtos.LoginResponse;
import com.library.user_service.dtos.RegisterExternalRequest;
import com.library.user_service.dtos.UserResponse;
import com.library.user_service.exception.BusinessException;
import com.library.user_service.exception.ConflictException;
import com.library.user_service.exception.UnauthorizedException;
import com.library.user_service.model.Role;
import com.library.user_service.model.User;
import com.library.user_service.model.Usertype;
import com.library.user_service.repository.UserRepository;
import com.library.user_service.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid credentials";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    // Compared when the e-mail does not exist, so the response time does not reveal it
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    // Same 401 for unknown e-mail, wrong password and account without password (counter-only member)
    public LoginResponse login(LoginRequest request) {
        Optional<User> found = userRepository.findByEmail(request.email());
        String hash = found.map(User::getPassword).orElse(null);
        boolean matches = passwordEncoder.matches(request.password(), hash != null ? hash : dummyHash);
        if (found.isEmpty() || hash == null || !matches) {
            throw new UnauthorizedException(INVALID_CREDENTIALS);
        }
        User user = found.get();
        return new LoginResponse(jwtService.generateToken(user), UserResponse.from(user));
    }

    @Transactional
    public UserResponse registerExternal(RegisterExternalRequest request) {
        Optional<User> sameCpf = userRepository.findByCpf(request.cpf());
        if (sameCpf.isPresent()) {
            // Account created at the counter: activate it instead of creating a new one
            User existing = sameCpf.get();
            if (!canActivate(existing, request.email(), Usertype.EXTERNAL)) {
                throw new ConflictException("CPF already registered");
            }
            existing.setPassword(passwordEncoder.encode(request.password()));
            return UserResponse.from(userRepository.save(existing));
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already registered");
        }
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setCpf(request.cpf());
        user.setType(Usertype.EXTERNAL);
        user.setRole(Role.MEMBER);
        user.setPassword(passwordEncoder.encode(request.password()));
        return UserResponse.from(userRepository.save(user));
    }

    /** A counter account can be activated by its owner: no password yet, same e-mail and same type. */
    static boolean canActivate(User existing, String email, Usertype type) {
        return existing.getRole() == Role.MEMBER
                && existing.getPassword() == null
                && existing.getType() == type
                && existing.getEmail().equalsIgnoreCase(email);
    }
}
