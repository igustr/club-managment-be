package ee.finalthesis.clubmanagement.service.dto.user;

import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddUserToClubDTO {

  @NotNull private UUID userId;

  @NotNull private ClubRole role;
}
