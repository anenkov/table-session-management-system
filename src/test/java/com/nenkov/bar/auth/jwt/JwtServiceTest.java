package com.nenkov.bar.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

class JwtServiceTest {

  @Test
  void generateAccessTokenBuildsExpectedClaims() {
    JwtEncoder encoder = org.mockito.Mockito.mock(JwtEncoder.class);
    JwtProperties props = new JwtProperties("https://test-issuer", 3600, "test-audience");

    JwtService service = new JwtService(encoder, props);

    Jwt jwt =
        new Jwt(
            "token-value",
            Instant.now(),
            Instant.now().plusSeconds(3600),
            Map.of("alg", "HS256"),
            Map.of("sub", "user1") // any non-empty claims map
            );

    when(encoder.encode(any())).thenReturn(jwt);

    String token = service.generateAccessToken("user1", "ADMIN");

    assertThat(token).isEqualTo("token-value");

    ArgumentCaptor<JwtEncoderParameters> captor =
        ArgumentCaptor.forClass(JwtEncoderParameters.class);

    verify(encoder).encode(captor.capture());

    JwtClaimsSet claims = captor.getValue().getClaims();

    assertThat(claims.getIssuer()).hasToString("https://test-issuer");
    assertThat(claims.getSubject()).isEqualTo("user1");
    assertThat(claims.getClaims()).containsEntry("role", "ADMIN");
    assertThat(claims.getExpiresAt()).isAfter(claims.getIssuedAt());
  }

  @Test
  void generateAccessTokenUsesHs256Algorithm() {
    JwtEncoder encoder = org.mockito.Mockito.mock(JwtEncoder.class);
    JwtProperties props = new JwtProperties("https://test-issuer", 3600, "audience");

    JwtService service = new JwtService(encoder, props);

    when(encoder.encode(any()))
        .thenReturn(
            new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "HS256"),
                Map.of("sub", "user1") // any non-empty claims map
                ));

    service.generateAccessToken("user", "ADMIN");

    ArgumentCaptor<JwtEncoderParameters> captor =
        ArgumentCaptor.forClass(JwtEncoderParameters.class);

    verify(encoder).encode(captor.capture());

    var header = captor.getValue().getJwsHeader();
    assertThat(header).isNotNull();
    assertThat(header.getAlgorithm()).isEqualTo(MacAlgorithm.HS256);
  }
}
