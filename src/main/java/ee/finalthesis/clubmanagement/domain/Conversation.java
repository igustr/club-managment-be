package ee.finalthesis.clubmanagement.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ee.finalthesis.clubmanagement.domain.enumeration.ConversationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "conversation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation extends AbstractAuditingEntity<UUID> implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull @Enumerated(EnumType.STRING)
  @Column(name = "type", length = 20, nullable = false)
  private ConversationType type;

  @Size(max = 1000) @Column(name = "last_message_text", length = 1000)
  private String lastMessageText;

  @Column(name = "last_message_time")
  private Instant lastMessageTime;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id")
  @JsonIgnoreProperties(
      value = {"teamMembers", "trainingSessions", "club"},
      allowSetters = true)
  private Team team;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "club_id", nullable = false)
  @JsonIgnoreProperties(
      value = {"users", "teams", "pitches"},
      allowSetters = true)
  private Club club;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "last_message_sender_id")
  @JsonIgnoreProperties(
      value = {"club", "parents", "children"},
      allowSetters = true)
  private User lastMessageSender;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "conversation")
  @JsonIgnoreProperties(
      value = {"conversation"},
      allowSetters = true)
  @Builder.Default
  private Set<ConversationParticipant> participants = new HashSet<>();

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "conversation")
  @JsonIgnoreProperties(
      value = {"conversation"},
      allowSetters = true)
  @Builder.Default
  private Set<Message> messages = new HashSet<>();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Conversation)) return false;
    return getId() != null && getId().equals(((Conversation) o).getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
