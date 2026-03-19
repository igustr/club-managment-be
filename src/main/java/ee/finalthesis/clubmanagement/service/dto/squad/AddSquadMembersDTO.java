package ee.finalthesis.clubmanagement.service.dto.squad;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddSquadMembersDTO {

  @NotEmpty private List<UUID> userIds;
}
