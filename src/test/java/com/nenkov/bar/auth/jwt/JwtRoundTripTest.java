package com.nenkov.bar.auth.jwt;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
class JwtRoundTripTest {

  @Autowired private JwtEncoder jwtEncoder;

  @Autowired private ReactiveJwtDecoder reactiveJwtDecoder;

  @Autowired private JwtProperties props;

  @Test
  void shouldEncodeAndDecodeJwt() {
    Instant now = Instant.now();

    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(props.issuer())
            .subject("test-user")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(60))
            .claim("role", "ROLE_USER")
            .build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

    Mono<Jwt> decoded = reactiveJwtDecoder.decode(token);

    StepVerifier.create(decoded)
        .assertNext(
            jwt -> {
              if (!"test-user".equals(jwt.getSubject())) {
                throw new AssertionError("subject mismatch");
              }
              if (jwt.getIssuer() == null || !props.issuer().equals(jwt.getIssuer().toString())) {
                throw new AssertionError("issuer mismatch");
              }
              if (!"ROLE_USER".equals(jwt.getClaimAsString("role"))) {
                throw new AssertionError("role mismatch");
              }
            })
        .verifyComplete();
  }
}
