package ee.finalthesis.clubmanagement.service.dto.chat;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateDirectConversationDTO {

  @NotNull private UUID participantId;
}
