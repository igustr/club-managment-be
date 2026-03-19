package ee.finalthesis.clubmanagement.service.dto.squad;

import ee.finalthesis.clubmanagement.domain.enumeration.AttendanceStatus;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.domain.enumeration.PlayerPosition;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SquadMemberDTO {

  private UUID id;
  private UUID userId;
  private String firstName;
  private String lastName;
  private String email;
  private ClubRole role;
  private PlayerPosition position;
  private AttendanceStatus status;
}
