package ee.finalthesis.clubmanagement.security;

import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityUtils {

  public static Optional<UUID> getCurrentUserId() {
    return getPrincipal().map(UserPrincipal::getId);
  }

  public static Optional<String> getCurrentUserEmail() {
    return getPrincipal().map(UserPrincipal::getEmail);
  }

  public static Optional<ClubRole> getCurrentUserRole() {
    return getPrincipal().map(UserPrincipal::getRole);
  }

  public static Optional<UUID> getCurrentUserClubId() {
    return getPrincipal().map(UserPrincipal::getClubId);
  }

  public static boolean isAuthenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null
        && authentication.isAuthenticated()
        && authentication.getPrincipal() instanceof UserPrincipal;
  }

  private static Optional<UserPrincipal> getPrincipal() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.getPrincipal() instanceof UserPrincipal principal) {
      return Optional.of(principal);
    }
    return Optional.empty();
  }
}
