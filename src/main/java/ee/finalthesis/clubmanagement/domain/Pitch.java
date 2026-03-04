package ee.finalthesis.clubmanagement.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "pitch")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pitch extends AbstractAuditingEntity<UUID> implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank @Size(max = 255) @Column(name = "name", nullable = false)
  private String name;

  @Size(max = 500) @Column(name = "address", length = 500)
  private String address;

  @Size(max = 100) @Column(name = "surface_type", length = 100)
  private String surfaceType;

  @Column(name = "capacity")
  private Integer capacity;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "club_id", nullable = false)
  @JsonIgnoreProperties(
      value = {"users", "teams", "pitches"},
      allowSetters = true)
  private Club club;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Pitch)) return false;
    return getId() != null && getId().equals(((Pitch) o).getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
