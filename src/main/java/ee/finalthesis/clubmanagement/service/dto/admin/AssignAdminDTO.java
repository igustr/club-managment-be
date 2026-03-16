package ee.finalthesis.clubmanagement.service.dto.admin;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignAdminDTO {

  @NotNull private UUID userId;
}
