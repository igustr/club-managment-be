package ee.finalthesis.clubmanagement.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;
import lombok.*;

@Entity
@Table(
    name = "team_member",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uc_team_member_team_id_user_id",
            columnNames = {"team_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMember extends AbstractAuditingEntity<UUID> implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "joined_date")
  private LocalDate joinedDate;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "team_id", nullable = false)
  @JsonIgnoreProperties(
      value = {"teamMembers", "trainingSessions", "club"},
      allowSetters = true)
  private Team team;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  @JsonIgnoreProperties(
      value = {"club", "parents", "children"},
      allowSetters = true)
  private User user;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TeamMember)) return false;
    return getId() != null && getId().equals(((TeamMember) o).getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
