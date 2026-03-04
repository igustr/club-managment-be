package ee.finalthesis.clubmanagement.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ee.finalthesis.clubmanagement.domain.enumeration.AppRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends AbstractAuditingEntity<UUID> implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank @Size(max = 255) @Column(name = "email", nullable = false, unique = true)
  private String email;

  @JsonIgnore
  @NotBlank @Size(max = 255) @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @NotNull @Enumerated(EnumType.STRING)
  @Column(name = "role", length = 20, nullable = false)
  private AppRole role;

  @NotNull @Column(name = "active", nullable = false)
  @Builder.Default
  private Boolean active = true;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "club_id")
  @JsonIgnoreProperties(
      value = {"users", "teams", "members", "fields"},
      allowSetters = true)
  private Club club;

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof User)) return false;
    return getId() != null && getId().equals(((User) o).getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
