package ee.finalthesis.clubmanagement.service.dto.chat;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParticipantDTO {

  private UUID userId;
  private String firstName;
  private String lastName;
  private String email;
}
