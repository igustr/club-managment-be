package ee.finalthesis.clubmanagement.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "message")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message extends AbstractAuditingEntity<UUID> implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank @Column(name = "text", columnDefinition = "TEXT", nullable = false)
  private String text;

  @NotNull @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "conversation_id", nullable = false)
  @JsonIgnoreProperties(
      value = {"participants", "messages"},
      allowSetters = true)
  private Conversation conversation;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sender_id", nullable = false)
  @JsonIgnoreProperties(
      value = {"club", "parents", "children"},
      allowSetters = true)
  private User sender;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Message)) return false;
    return getId() != null && getId().equals(((Message) o).getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
