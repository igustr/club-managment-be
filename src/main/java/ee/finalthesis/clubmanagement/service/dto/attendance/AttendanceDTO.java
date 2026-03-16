package ee.finalthesis.clubmanagement.service.dto.attendance;

import ee.finalthesis.clubmanagement.domain.enumeration.AttendanceStatus;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.domain.enumeration.PlayerPosition;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttendanceDTO {

  private UUID id;
  private UUID trainingSessionId;
  private UUID userId;
  private String firstName;
  private String lastName;
  private String email;
  private ClubRole role;
  private PlayerPosition position;
  private AttendanceStatus status;
}
