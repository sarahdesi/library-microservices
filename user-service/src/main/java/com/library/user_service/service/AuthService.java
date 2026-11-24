package com.library.user_service.service;

import com.library.user_service.dtos.LoginRequest;
import com.library.user_service.dtos.LoginResponse;
import com.library.user_service.dtos.RegisterExternalRequest;
import com.library.user_service.dtos.RegisterUniversityRequest;
import com.library.user_service.dtos.UserResponse;
import com.library.user_service.exception.BusinessException;
import com.library.user_service.exception.ConflictException;
import com.library.user_service.exception.ForbiddenException;
import com.library.user_service.exception.UnauthorizedException;
import com.library.user_service.model.Role;
import com.library.user_service.model.User;
import com.library.user_service.model.Usertype;
import com.library.user_service.repository.UserRepository;
import com.library.user_service.security.JwtService;
import com.library.user_service.university.UniversityPerson;
import com.library.user_service.university.UniversityRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Invalid credentials";
    private static final String DATA_DOES_NOT_MATCH = "University data does not match";
    private static final String AFFILIATION_INACTIVE = "University affiliation is no longer active";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UniversityRegistry universityRegistry;
    // Compared when the e-mail does not exist, so the response time does not reveal it
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       UniversityRegistry universityRegistry) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.universityRegistry = universityRegistry;
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
        checkUniversityAffiliation(user);
        return new LoginResponse(jwtService.generateToken(user), UserResponse.from(user));
    }

    /**
     * Students and teachers are checked against the university at every login (after the password),
     * so someone who graduated or left cannot get a new token. Registry down: 503, no token.
     */
    private void checkUniversityAffiliation(User user) {
        if (user.getType() != Usertype.STUDENT && user.getType() != Usertype.TEACHER) {
            return;
        }
        if (user.getRegistrationNumber() == null || user.getCpf() == null
                || findInUniversity(user.getType(), user.getRegistrationNumber(), user.getCpf()).isEmpty()) {
            throw new ForbiddenException(AFFILIATION_INACTIVE);
        }
    }

    @Transactional
    public UserResponse registerStudent(RegisterUniversityRequest request) {
        return registerUniversityMember(request, Usertype.STUDENT);
    }

    @Transactional
    public UserResponse registerTeacher(RegisterUniversityRequest request) {
        return registerUniversityMember(request, Usertype.TEACHER);
    }

    private UserResponse registerUniversityMember(RegisterUniversityRequest request, Usertype type) {
        String number = request.registrationNumber().trim();

        // 1. The university decides who is a student/teacher (separate bases per type).
        //    Checked first, so conflicts below are only revealed to someone who knows number + CPF.
        UniversityPerson person = findInUniversity(type, number, request.cpf())
                .orElseThrow(() -> new BusinessException(DATA_DOES_NOT_MATCH));

        // 2. Counter account with the same CPF: activate it
        Optional<User> sameCpf = userRepository.findByCpf(request.cpf());
        if (sameCpf.isPresent()) {
            User existing = sameCpf.get();
            if (!canActivate(existing, request.email(), type)) {
                throw new ConflictException("CPF already registered");
            }
            if (userRepository.existsByTypeAndRegistrationNumber(type, number)) {
                throw new ConflictException("Registration number already registered");
            }
            applyUniversityData(existing, person, number);
            existing.setPassword(passwordEncoder.encode(request.password()));
            return UserResponse.from(userRepository.save(existing));
        }

        if (userRepository.existsByTypeAndRegistrationNumber(type, number)) {
            throw new ConflictException("Registration number already registered");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email already registered");
        }

        User user = new User();
        user.setEmail(request.email());
        user.setCpf(request.cpf());
        user.setType(type);
        user.setRole(Role.MEMBER);
        user.setPassword(passwordEncoder.encode(request.password()));
        applyUniversityData(user, person, number);
        return UserResponse.from(userRepository.save(user));
    }

    // The name always comes from the university, never from what the person typed
    private static void applyUniversityData(User user, UniversityPerson person, String number) {
        user.setName(person.name());
        user.setRegistrationNumber(number);
    }

    private Optional<UniversityPerson> findInUniversity(Usertype type, String number, String cpf) {
        return type == Usertype.TEACHER
                ? universityRegistry.findActiveTeacher(number, cpf)
                : universityRegistry.findActiveStudent(number, cpf);
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
