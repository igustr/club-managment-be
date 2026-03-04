package ee.finalthesis.clubmanagement.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ee.finalthesis.clubmanagement.domain.enumeration.NotificationType;
import ee.finalthesis.clubmanagement.domain.enumeration.RecipientGroup;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "notification")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends AbstractAuditingEntity<UUID> implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank @Size(max = 255) @Column(name = "title", nullable = false)
  private String title;

  @NotBlank @Column(name = "content", columnDefinition = "TEXT", nullable = false)
  private String content;

  @NotNull @Enumerated(EnumType.STRING)
  @Column(name = "notification_type", length = 30, nullable = false)
  private NotificationType notificationType;

  @NotNull @Enumerated(EnumType.STRING)
  @Column(name = "recipient_group", length = 20, nullable = false)
  private RecipientGroup recipientGroup;

  @Column(name = "sent_at")
  private Instant sentAt;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "club_id", nullable = false)
  @JsonIgnoreProperties(
      value = {"users", "teams", "members", "fields"},
      allowSetters = true)
  private Club club;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "team_id")
  @JsonIgnoreProperties(
      value = {"teamMembers", "trainingSessions", "club", "coach"},
      allowSetters = true)
  private Team team;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "sender_id", nullable = false)
  @JsonIgnoreProperties(
      value = {"club"},
      allowSetters = true)
  private User sender;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "notification")
  @JsonIgnoreProperties(
      value = {"notification"},
      allowSetters = true)
  @Builder.Default
  private Set<NotificationRecipient> recipients = new HashSet<>();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Notification)) return false;
    return getId() != null && getId().equals(((Notification) o).getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
