package com.nenkov.bar.web.api.auth;

import com.nenkov.bar.auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/auth/login")
  public Mono<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request.username(), request.password());
  }
}
