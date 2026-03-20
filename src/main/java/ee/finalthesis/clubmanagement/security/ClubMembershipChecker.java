package ee.finalthesis.clubmanagement.security;

import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.domain.enumeration.SystemRole;
import ee.finalthesis.clubmanagement.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("clubSecurity")
@RequiredArgsConstructor
public class ClubMembershipChecker {

  private final UserRepository userRepository;

  /** Checks if the current user is a MASTER_ADMIN (platform-level). */
  public boolean isMasterAdmin() {
    return SecurityUtils.getCurrentUserSystemRole()
        .map(SystemRole.MASTER_ADMIN::equals)
        .orElse(false);
  }

  /**
   * Checks if the current authenticated user belongs to the given club. Users with no club
   * assignment (role/club_id is null) are denied. Master admins bypass this check.
   */
  public boolean isMemberOfClub(UUID clubId) {
    if (isMasterAdmin()) {
      return true;
    }
    if (clubId == null) {
      return false;
    }
    return SecurityUtils.getCurrentUserClubId().map(clubId::equals).orElse(false);
  }

  /** Checks if the current user is a CLUB_ADMIN of the given club. Master admins bypass. */
  public boolean isAdmin(UUID clubId) {
    if (isMasterAdmin()) {
      return true;
    }
    return isMemberOfClub(clubId)
        && SecurityUtils.getCurrentUserRole().map(ClubRole.CLUB_ADMIN::equals).orElse(false);
  }

  /**
   * Checks if the current user is a CLUB_ADMIN or COACH of the given club. Master admins bypass.
   */
  public boolean isAdminOrCoach(UUID clubId) {
    if (isMasterAdmin()) {
      return true;
    }
    if (!isMemberOfClub(clubId)) {
      return false;
    }
    return SecurityUtils.getCurrentUserRole()
        .map(role -> role == ClubRole.CLUB_ADMIN || role == ClubRole.COACH)
        .orElse(false);
  }

  /**
   * Checks if the current user is an admin/coach of the club OR a parent of the given user.
   */
  public boolean isAdminOrCoachOrParentOf(UUID clubId, UUID userId) {
    if (isAdminOrCoach(clubId)) {
      return true;
    }
    if (!isMemberOfClub(clubId)) {
      return false;
    }
    ClubRole role = SecurityUtils.getCurrentUserRole().orElse(null);
    if (role != ClubRole.PARENT) {
      return false;
    }
    UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (currentUserId == null) {
      return false;
    }
    return userRepository.existsParentChildRelationship(userId, currentUserId);
  }

  /** Checks if the current user has the CLUB_ADMIN role (regardless of club) or is MASTER_ADMIN. */
  public boolean isAnyAdmin() {
    if (isMasterAdmin()) {
      return true;
    }
    return SecurityUtils.getCurrentUserRole().map(ClubRole.CLUB_ADMIN::equals).orElse(false);
  }
}
