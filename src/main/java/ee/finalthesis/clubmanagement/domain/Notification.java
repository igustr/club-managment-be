package ee.finalthesis.clubmanagement.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ee.finalthesis.clubmanagement.domain.enumeration.NotificationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
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

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  @JsonIgnoreProperties(
      value = {"club", "parents", "children"},
      allowSetters = true)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "club_id", nullable = false)
  @JsonIgnoreProperties(
      value = {"users"},
      allowSetters = true)
  private Club club;

  @NotNull @Enumerated(EnumType.STRING)
  @Column(name = "type", length = 30, nullable = false)
  private NotificationType type;

  @NotBlank @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "message", length = 500)
  private String message;

  @Column(name = "reference_id")
  private UUID referenceId;

  @NotNull @Column(name = "is_read", nullable = false)
  @Builder.Default
  private boolean read = false;

  @NotNull @Column(name = "created_at", nullable = false)
  @Builder.Default
  private Instant createdAt = Instant.now();

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
