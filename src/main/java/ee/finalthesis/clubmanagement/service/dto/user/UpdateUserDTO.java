package ee.finalthesis.clubmanagement.service.dto.user;

import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserDTO {

  @Size(max = 100) private String firstName;

  @Size(max = 100) private String lastName;

  @Size(max = 50) private String phone;

  @Size(max = 500) private String photoUrl;

  private ClubRole role;

  private Boolean active;
}
