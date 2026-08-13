package com.weddingshare.auth;

import com.weddingshare.user.User;
import com.weddingshare.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    public Optional<LoginResponse> login(LoginRequest request) {
        if (request == null || !StringUtils.hasText(request.email()) || !StringUtils.hasText(request.password())) {
            return Optional.empty();
        }

        Optional<User> user = userRepository.findByEmail(request.email());
        if (user.isEmpty() || !passwordEncoder.matches(request.password(), user.get().getPasswordHash())) {
            return Optional.empty();
        }

        return Optional.of(jwtTokenService.createToken(user.get()));
    }
}
