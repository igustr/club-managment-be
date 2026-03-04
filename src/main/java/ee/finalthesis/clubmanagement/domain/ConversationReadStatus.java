package ee.finalthesis.clubmanagement.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(
    name = "conversation_read_status",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uc_conversation_read_status_conv_id_user_id",
            columnNames = {"conversation_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationReadStatus extends AbstractAuditingEntity<UUID> implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull @Column(name = "unread_count", nullable = false)
  @Builder.Default
  private Integer unreadCount = 0;

  @Column(name = "last_read_at")
  private Instant lastReadAt;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "conversation_id", nullable = false)
  @JsonIgnoreProperties(
      value = {"participants", "messages"},
      allowSetters = true)
  private Conversation conversation;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  @JsonIgnoreProperties(
      value = {"club", "parents", "children"},
      allowSetters = true)
  private User user;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ConversationReadStatus)) return false;
    return getId() != null && getId().equals(((ConversationReadStatus) o).getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
