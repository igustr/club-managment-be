package ee.finalthesis.clubmanagement.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ee.finalthesis.clubmanagement.domain.enumeration.AttendanceStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(
    name = "attendance",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uc_attendance_training_session_id_member_id",
            columnNames = {"training_session_id", "member_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attendance extends AbstractAuditingEntity<UUID> implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  @Builder.Default
  private AttendanceStatus status = AttendanceStatus.PENDING;

  @Column(name = "confirmed_at")
  private Instant confirmedAt;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "training_session_id", nullable = false)
  @JsonIgnoreProperties(
      value = {"attendances", "team", "field"},
      allowSetters = true)
  private TrainingSession trainingSession;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "member_id", nullable = false)
  @JsonIgnoreProperties(
      value = {"teamMembers", "club", "user", "parent"},
      allowSetters = true)
  private Member member;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "confirmed_by_user_id")
  @JsonIgnoreProperties(
      value = {"club"},
      allowSetters = true)
  private User confirmedByUser;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Attendance)) return false;
    return getId() != null && getId().equals(((Attendance) o).getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
