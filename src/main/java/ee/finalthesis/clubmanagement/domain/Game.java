package ee.finalthesis.clubmanagement.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ee.finalthesis.clubmanagement.domain.enumeration.GameStatus;
import ee.finalthesis.clubmanagement.domain.enumeration.VenueType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
@Table(name = "game")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Game extends AbstractAuditingEntity<UUID> implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull @Column(name = "date", nullable = false)
  private LocalDate date;

  @Column(name = "gathering_time")
  private LocalTime gatheringTime;

  @NotNull @Column(name = "start_time", nullable = false)
  private LocalTime startTime;

  @NotNull @Column(name = "end_time", nullable = false)
  private LocalTime endTime;

  @NotBlank @Size(max = 255) @Column(name = "opponent", nullable = false)
  private String opponent;

  @NotNull @Enumerated(EnumType.STRING)
  @Column(name = "venue_type", length = 20, nullable = false)
  private VenueType venueType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pitch_id")
  @JsonIgnoreProperties(
      value = {"club"},
      allowSetters = true)
  private Pitch pitch;

  @Size(max = 255) @Column(name = "venue_name")
  private String venueName;

  @Size(max = 500) @Column(name = "venue_address", length = 500)
  private String venueAddress;

  @NotNull @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  @Builder.Default
  private GameStatus status = GameStatus.SCHEDULED;

  @Size(max = 1000) @Column(name = "notes", length = 1000)
  private String notes;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "team_id", nullable = false)
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

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "game")
  @JsonIgnoreProperties(
      value = {"game"},
      allowSetters = true)
  @Builder.Default
  private Set<GameSquadMember> squadMembers = new HashSet<>();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Game)) return false;
    return getId() != null && getId().equals(((Game) o).getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
