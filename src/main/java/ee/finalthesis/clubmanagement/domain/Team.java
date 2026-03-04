package ee.finalthesis.clubmanagement.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "team")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Team extends AbstractAuditingEntity<UUID> implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank @Size(max = 255) @Column(name = "name", nullable = false)
  private String name;

  @Size(max = 50) @Column(name = "age_group", length = 50)
  private String ageGroup;

  @Size(max = 20) @Column(name = "season", length = 20)
  private String season;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "club_id", nullable = false)
  @JsonIgnoreProperties(
      value = {"users", "teams", "pitches"},
      allowSetters = true)
  private Club club;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "team")
  @JsonIgnoreProperties(
      value = {"team"},
      allowSetters = true)
  @Builder.Default
  private Set<TeamMember> teamMembers = new HashSet<>();

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "team")
  @JsonIgnoreProperties(
      value = {"team"},
      allowSetters = true)
  @Builder.Default
  private Set<TrainingSession> trainingSessions = new HashSet<>();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Team)) return false;
    return getId() != null && getId().equals(((Team) o).getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
