package ee.finalthesis.clubmanagement.security;

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
}
