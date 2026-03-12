package ee.finalthesis.clubmanagement.service.dto.chat;

import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageDTO {

  private UUID id;
  private UUID conversationId;
  private UUID senderId;
  private String senderFirstName;
  private String senderLastName;
  private String text;
  private Instant createdAt;
}
