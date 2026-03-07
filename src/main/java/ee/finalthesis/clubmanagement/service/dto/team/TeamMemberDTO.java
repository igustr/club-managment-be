package ee.finalthesis.clubmanagement.service.dto.team;

import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamMemberDTO {

  private UUID id;
  private UUID userId;
  private UUID teamId;
  private String firstName;
  private String lastName;
  private String email;
  private ClubRole role;
  private LocalDate joinedDate;
}
