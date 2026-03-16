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

  /**
   * Checks if the current user can view a team. ADMIN can view all teams in their club. COACH can
   * only view teams they belong to. PLAYER/PARENT can view teams they belong to.
   */
  public boolean canAccessTeam(UUID teamId) {
    if (teamId == null) {
      return false;
    }
    if (isMasterAdmin()) {
      return true;
    }
    UUID userId = SecurityUtils.getCurrentUserId().orElse(null);
    if (userId == null) {
      return false;
    }

    ClubRole role = SecurityUtils.getCurrentUserRole().orElse(null);
    if (role == ClubRole.CLUB_ADMIN) {
      return true;
    }

    return teamMemberRepository.existsByTeamIdAndUserId(teamId, userId);
  }

  /**
   * Checks if the current user can manage a team (create/edit trainings, manage roster). Only
   * CLUB_ADMIN and COACH of that team can manage.
   */
  public boolean canManageTeam(UUID teamId) {
    if (teamId == null) {
      return false;
    }
    if (isMasterAdmin()) {
      return true;
    }
    UUID userId = SecurityUtils.getCurrentUserId().orElse(null);
    if (userId == null) {
      return false;
    }

    ClubRole role = SecurityUtils.getCurrentUserRole().orElse(null);
    if (role == ClubRole.CLUB_ADMIN) {
      return true;
    }
    if (role == ClubRole.COACH) {
      return teamMemberRepository.existsByTeamIdAndUserId(teamId, userId);
    }

    return false;
  }

  private boolean isMasterAdmin() {
    return SecurityUtils.getCurrentUserSystemRole()
        .map(SystemRole.MASTER_ADMIN::equals)
        .orElse(false);
  }
}
