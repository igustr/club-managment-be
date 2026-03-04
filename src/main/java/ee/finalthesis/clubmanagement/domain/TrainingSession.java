package ee.finalthesis.clubmanagement.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ee.finalthesis.clubmanagement.domain.enumeration.TrainingSessionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "training_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrainingSession extends AbstractAuditingEntity<UUID> implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull @Column(name = "date", nullable = false)
  private LocalDate date;

  @NotNull @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @NotNull @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  @Column(name = "recurrence_group_id")
  private UUID recurrenceGroupId;

  @NotNull @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  @Builder.Default
  private TrainingSessionStatus status = TrainingSessionStatus.SCHEDULED;

  @Size(max = 1000) @Column(name = "notes", length = 1000)
  private String notes;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "team_id", nullable = false)
  @JsonIgnoreProperties(
      value = {"teamMembers", "trainingSessions", "club"},
      allowSetters = true)
  private Team team;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pitch_id")
  @JsonIgnoreProperties(
      value = {"club"},
      allowSetters = true)
  private Pitch pitch;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "trainingSession")
  @JsonIgnoreProperties(
      value = {"trainingSession"},
      allowSetters = true)
  @Builder.Default
  private Set<Attendance> attendances = new HashSet<>();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TrainingSession)) return false;
    return getId() != null && getId().equals(((TrainingSession) o).getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
