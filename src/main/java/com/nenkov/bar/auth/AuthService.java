package com.nenkov.bar.auth;

import com.nenkov.bar.auth.jwt.JwtService;
import com.nenkov.bar.user.UserEntity;
import com.nenkov.bar.user.UserRepository;
import com.nenkov.bar.web.api.auth.LoginResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(
      UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  public Mono<LoginResponse> login(String username, String rawPassword) {
    return userRepository
        .findByUsername(username)
        .filter(UserEntity::active)
        .filter(user -> passwordEncoder.matches(rawPassword, user.passwordHash()))
        .switchIfEmpty(Mono.error(new InvalidCredentialsException()))
        .map(
            user ->
                new LoginResponse(
                    jwtService.generateAccessToken(user.username(), user.role()),
                    jwtService.ttlSeconds()));
  }
}
