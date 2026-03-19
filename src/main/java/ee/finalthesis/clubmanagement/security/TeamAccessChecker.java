package ee.finalthesis.clubmanagement.security;

import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.domain.enumeration.SystemRole;
import ee.finalthesis.clubmanagement.repository.TeamMemberRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("teamSecurity")
@RequiredArgsConstructor
public class TeamAccessChecker {

  private final TeamMemberRepository teamMemberRepository;

  /** MASTER_ADMIN and CLUB_ADMIN can view all teams. Others must be a member. */
  public boolean canAccessTeam(UUID teamId) {
    Boolean adminCheck = checkAdminAccess(teamId);
    if (adminCheck != null) {
      return adminCheck;
    }
    UUID userId = SecurityUtils.getCurrentUserId().orElse(null);
    return teamMemberRepository.existsByTeamIdAndUserId(teamId, userId);
  }

  /** MASTER_ADMIN and CLUB_ADMIN can manage all teams. COACH can manage teams they belong to. */
  public boolean canManageTeam(UUID teamId) {
    Boolean adminCheck = checkAdminAccess(teamId);
    if (adminCheck != null) {
      return adminCheck;
    }
    if (SecurityUtils.getCurrentUserRole().orElse(null) == ClubRole.COACH) {
      UUID userId = SecurityUtils.getCurrentUserId().orElse(null);
      return teamMemberRepository.existsByTeamIdAndUserId(teamId, userId);
    }
    return false;
  }

  /**
   * Returns true for admins, false for invalid state, or null when the caller must do its own
   * check.
   */
  private Boolean checkAdminAccess(UUID teamId) {
    if (teamId == null || SecurityUtils.getCurrentUserId().orElse(null) == null) {
      return false;
    }
    if (SecurityUtils.getCurrentUserSystemRole().map(SystemRole.MASTER_ADMIN::equals).orElse(false)
        || SecurityUtils.getCurrentUserRole().orElse(null) == ClubRole.CLUB_ADMIN) {
      return true;
    }
    return null;
  }
}
