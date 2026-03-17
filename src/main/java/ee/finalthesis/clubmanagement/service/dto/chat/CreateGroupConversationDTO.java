package ee.finalthesis.clubmanagement.service.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateGroupConversationDTO {

  @NotBlank @Size(max = 200) private String name;

  @NotEmpty private List<UUID> participantIds;
}
