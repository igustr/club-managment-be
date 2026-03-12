package ee.finalthesis.clubmanagement.security;

import static org.assertj.core.api.Assertions.assertThat;

import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class SecurityUtilsTest {

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void getCurrentUserId_withAuthenticated_shouldReturnId() {
    UUID userId = UUID.randomUUID();
    setAuthentication(userId, "user@test.com", ClubRole.PLAYER, UUID.randomUUID());

    assertThat(SecurityUtils.getCurrentUserId()).isPresent().contains(userId);
  }

  @Test
  void getCurrentUserId_withNoAuth_shouldReturnEmpty() {
    assertThat(SecurityUtils.getCurrentUserId()).isEmpty();
  }

  @Test
  void getCurrentUserEmail_shouldReturnEmail() {
    setAuthentication(UUID.randomUUID(), "user@test.com", ClubRole.PLAYER, UUID.randomUUID());

    assertThat(SecurityUtils.getCurrentUserEmail()).isPresent().contains("user@test.com");
  }

  @Test
  void getCurrentUserRole_shouldReturnRole() {
    setAuthentication(UUID.randomUUID(), "user@test.com", ClubRole.COACH, UUID.randomUUID());

    assertThat(SecurityUtils.getCurrentUserRole()).isPresent().contains(ClubRole.COACH);
  }

  @Test
  void getCurrentUserClubId_shouldReturnClubId() {
    UUID clubId = UUID.randomUUID();
    setAuthentication(UUID.randomUUID(), "user@test.com", ClubRole.ADMIN, clubId);

    assertThat(SecurityUtils.getCurrentUserClubId()).isPresent().contains(clubId);
  }

  @Test
  void getCurrentUserClubId_withNullClub_shouldReturnEmpty() {
    setAuthentication(UUID.randomUUID(), "user@test.com", ClubRole.PLAYER, null);

    assertThat(SecurityUtils.getCurrentUserClubId()).isEmpty();
  }

  @Test
  void isAuthenticated_withUserPrincipal_shouldReturnTrue() {
    setAuthentication(UUID.randomUUID(), "user@test.com", ClubRole.ADMIN, UUID.randomUUID());

    assertThat(SecurityUtils.isAuthenticated()).isTrue();
  }

  @Test
  void isAuthenticated_withNoAuth_shouldReturnFalse() {
    assertThat(SecurityUtils.isAuthenticated()).isFalse();
  }

  @Test
  void isAuthenticated_withStringPrincipal_shouldReturnFalse() {
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken("anonymousUser", null);
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertThat(SecurityUtils.isAuthenticated()).isFalse();
  }

  private void setAuthentication(UUID userId, String email, ClubRole role, UUID clubId) {
    UserPrincipal principal = UserPrincipal.fromToken(userId, email, role, clubId);
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }
}
