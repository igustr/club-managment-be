package ee.finalthesis.clubmanagement.service.dto.auth;

import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.domain.enumeration.PlayerPosition;
import ee.finalthesis.clubmanagement.domain.enumeration.SystemRole;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDTO {

  private UUID id;
  private String email;
  private String firstName;
  private String lastName;
  private LocalDate dateOfBirth;
  private String phone;
  private String photoUrl;
  private ClubRole role;
  private PlayerPosition position;
  private SystemRole systemRole;
  private UUID clubId;
  private Boolean active;
}
