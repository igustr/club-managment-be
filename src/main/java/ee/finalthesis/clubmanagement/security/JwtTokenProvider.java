package ee.finalthesis.clubmanagement.security;

import ee.finalthesis.clubmanagement.config.SecurityProperties;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.domain.enumeration.SystemRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

  private static final String CLAIM_EMAIL = "email";
  private static final String CLAIM_ROLE = "role";
  private static final String CLAIM_CLUB_ID = "clubId";
  private static final String CLAIM_SYSTEM_ROLE = "systemRole";
  private static final String CLAIM_TYPE = "type";
  private static final String TOKEN_TYPE_ACCESS = "access";
  private static final String TOKEN_TYPE_REFRESH = "refresh";

  private final SecurityProperties securityProperties;
  private SecretKey key;

  @PostConstruct
  void init() {
    byte[] keyBytes = Decoders.BASE64.decode(securityProperties.getSecret());
    this.key = Keys.hmacShaKeyFor(keyBytes);
  }

  public String generateAccessToken(UserPrincipal principal) {
    long now = System.currentTimeMillis();
    long validity = securityProperties.getTokenValidityInSeconds() * 1000;

    return Jwts.builder()
        .subject(principal.getId().toString())
        .claim(CLAIM_EMAIL, principal.getEmail())
        .claim(CLAIM_ROLE, principal.getRole() != null ? principal.getRole().name() : null)
        .claim(
            CLAIM_CLUB_ID, principal.getClubId() != null ? principal.getClubId().toString() : null)
        .claim(
            CLAIM_SYSTEM_ROLE,
            principal.getSystemRole() != null ? principal.getSystemRole().name() : null)
        .claim(CLAIM_TYPE, TOKEN_TYPE_ACCESS)
        .issuedAt(new Date(now))
        .expiration(new Date(now + validity))
        .signWith(key)
        .compact();
  }

  public String generateRefreshToken(UserPrincipal principal) {
    long now = System.currentTimeMillis();
    long validity = securityProperties.getRefreshTokenValidityInSeconds() * 1000;

    return Jwts.builder()
        .subject(principal.getId().toString())
        .claim(CLAIM_TYPE, TOKEN_TYPE_REFRESH)
        .issuedAt(new Date(now))
        .expiration(new Date(now + validity))
        .signWith(key)
        .compact();
  }

  public Authentication getAuthentication(String token) {
    Claims claims = parseToken(token);
    UUID userId = UUID.fromString(claims.getSubject());
    String email = claims.get(CLAIM_EMAIL, String.class);
    String roleName = claims.get(CLAIM_ROLE, String.class);
    String clubIdStr = claims.get(CLAIM_CLUB_ID, String.class);
    String systemRoleName = claims.get(CLAIM_SYSTEM_ROLE, String.class);

    ClubRole role = roleName != null ? ClubRole.valueOf(roleName) : null;
    UUID clubId = clubIdStr != null ? UUID.fromString(clubIdStr) : null;
    SystemRole systemRole = systemRoleName != null ? SystemRole.valueOf(systemRoleName) : null;

    UserPrincipal principal =
        UserPrincipal.fromToken(userId, email, role, systemRole, clubId, true);
    return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
  }

  public boolean validateToken(String token) {
    try {
      parseToken(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      log.debug("Invalid JWT token: {}", e.getMessage());
      return false;
    }
  }

  public boolean isRefreshToken(String token) {
    try {
      Claims claims = parseToken(token);
      return TOKEN_TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  public UUID getUserIdFromToken(String token) {
    Claims claims = parseToken(token);
    return UUID.fromString(claims.getSubject());
  }

  private Claims parseToken(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
