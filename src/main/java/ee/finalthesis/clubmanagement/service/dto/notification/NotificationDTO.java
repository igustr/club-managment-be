package ee.finalthesis.clubmanagement.service.dto.notification;

import ee.finalthesis.clubmanagement.domain.enumeration.NotificationType;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationDTO {

  private UUID id;
  private NotificationType type;
  private String title;
  private String message;
  private UUID referenceId;
  private boolean read;
  private Instant createdAt;
}
