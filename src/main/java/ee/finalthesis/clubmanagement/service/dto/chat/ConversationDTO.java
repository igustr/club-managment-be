package ee.finalthesis.clubmanagement.service.dto.chat;

import ee.finalthesis.clubmanagement.domain.enumeration.ConversationType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConversationDTO {

  private UUID id;
  private ConversationType type;
  private String name;
  private UUID teamId;
  private String lastMessageText;
  private Instant lastMessageTime;
  private String lastMessageSenderName;
  private int unreadCount;
  private List<ParticipantDTO> participants;
}
