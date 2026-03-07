package ee.finalthesis.clubmanagement.security;

import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("clubSecurity")
@RequiredArgsConstructor
public class ClubMembershipChecker {

  /**
   * Checks if the current authenticated user belongs to the given club. Users with no club
   * assignment (role/club_id is null) are denied.
   */
  public boolean isMemberOfClub(UUID clubId) {
    if (clubId == null) {
      return false;
    }
    return SecurityUtils.getCurrentUserClubId().map(clubId::equals).orElse(false);
  }

  /** Checks if the current user is an ADMIN of the given club. */
  public boolean isAdmin(UUID clubId) {
    return isMemberOfClub(clubId)
        && SecurityUtils.getCurrentUserRole().map(ClubRole.ADMIN::equals).orElse(false);
  }

  /** Checks if the current user is an ADMIN or COACH of the given club. */
  public boolean isAdminOrCoach(UUID clubId) {
    if (!isMemberOfClub(clubId)) {
      return false;
    }
    return SecurityUtils.getCurrentUserRole()
        .map(role -> role == ClubRole.ADMIN || role == ClubRole.COACH)
        .orElse(false);
  }

  /** Checks if the current user has the ADMIN role (regardless of club). */
  public boolean isAnyAdmin() {
    return SecurityUtils.getCurrentUserRole().map(ClubRole.ADMIN::equals).orElse(false);
  }
}
