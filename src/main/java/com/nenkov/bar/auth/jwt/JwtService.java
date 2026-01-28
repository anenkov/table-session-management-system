package com.nenkov.bar.auth.jwt;

import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private final JwtEncoder jwtEncoder;
  private final JwtProperties props;

  public JwtService(JwtEncoder jwtEncoder, JwtProperties props) {
    this.jwtEncoder = jwtEncoder;
    this.props = props;
  }

  public String generateAccessToken(String username, String role) {
    Instant now = Instant.now();

    JwtClaimsSet claims =
        // Configures JWT claims with issuer, time, subject
        JwtClaimsSet.builder()
            .issuer(props.issuer())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(props.ttlSeconds()))
            .subject(username)
            .claim("role", role)
            .build();

    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }

  public long ttlSeconds() {
    return props.ttlSeconds();
  }
}
