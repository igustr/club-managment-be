package ee.finalthesis.clubmanagement.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ee.finalthesis.clubmanagement.domain.enumeration.AttendanceStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.UUID;
import lombok.*;

@Entity
@Table(
    name = "game_squad_member",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uc_game_squad_member_game_id_user_id",
            columnNames = {"game_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameSquadMember extends AbstractAuditingEntity<UUID> implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotNull @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  @Builder.Default
  private AttendanceStatus status = AttendanceStatus.PENDING;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "game_id", nullable = false)
  @JsonIgnoreProperties(
      value = {"squadMembers", "team", "pitch", "club"},
      allowSetters = true)
  private Game game;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  @JsonIgnoreProperties(
      value = {"club", "parents", "children"},
      allowSetters = true)
  private User user;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof GameSquadMember)) return false;
    return getId() != null && getId().equals(((GameSquadMember) o).getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
