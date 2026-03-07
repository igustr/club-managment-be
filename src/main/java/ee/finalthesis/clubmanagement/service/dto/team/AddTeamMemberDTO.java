package ee.finalthesis.clubmanagement.service.dto.team;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddTeamMemberDTO {

  @NotNull private UUID userId;
}
