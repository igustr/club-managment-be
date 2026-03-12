package ee.finalthesis.clubmanagement.service.dto.user;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinkParentDTO {

  @NotNull private UUID parentId;
}
