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
@Table(name = "club")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Club extends AbstractAuditingEntity<UUID> implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank @Size(max = 255) @Column(name = "name", nullable = false)
  private String name;

  @Size(max = 50) @Column(name = "registration_code", length = 50)
  private String registrationCode;

  @Size(max = 500) @Column(name = "address", length = 500)
  private String address;

  @Size(max = 255) @Column(name = "contact_email")
  private String contactEmail;

  @Size(max = 50) @Column(name = "contact_phone", length = 50)
  private String contactPhone;

  @Size(max = 500) @Column(name = "logo_url", length = 500)
  private String logoUrl;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "club")
  @JsonIgnoreProperties(
      value = {"club"},
      allowSetters = true)
  @Builder.Default
  private Set<User> users = new HashSet<>();

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "club")
  @JsonIgnoreProperties(
      value = {"club"},
      allowSetters = true)
  @Builder.Default
  private Set<Team> teams = new HashSet<>();

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "club")
  @JsonIgnoreProperties(
      value = {"club"},
      allowSetters = true)
  @Builder.Default
  private Set<Pitch> pitches = new HashSet<>();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Club)) return false;
    return getId() != null && getId().equals(((Club) o).getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
