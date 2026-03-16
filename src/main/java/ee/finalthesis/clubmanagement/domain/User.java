package ee.finalthesis.clubmanagement.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.domain.enumeration.SystemRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
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

  @NotBlank @Size(max = 100) @Column(name = "first_name", length = 100, nullable = false)
  private String firstName;

  @NotBlank @Size(max = 100) @Column(name = "last_name", length = 100, nullable = false)
  private String lastName;

  @NotNull @Column(name = "date_of_birth", nullable = false)
  private LocalDate dateOfBirth;

  @NotBlank @Size(max = 50) @Column(name = "phone", length = 50, nullable = false)
  private String phone;

  @Size(max = 500) @Column(name = "photo_url", length = 500)
  private String photoUrl;

  @Enumerated(EnumType.STRING)
  @Column(name = "role", length = 20)
  private ClubRole role;

  @Enumerated(EnumType.STRING)
  @Column(name = "system_role", length = 20)
  private SystemRole systemRole;

  @NotNull @Column(name = "active", nullable = false)
  @Builder.Default
  private Boolean active = true;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "club_id")
  @JsonIgnoreProperties(
      value = {"users", "teams", "pitches"},
      allowSetters = true)
  private Club club;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "user_parent",
      joinColumns = @JoinColumn(name = "child_id"),
      inverseJoinColumns = @JoinColumn(name = "parent_id"),
      uniqueConstraints =
          @UniqueConstraint(
              name = "uc_user_parent_child_id_parent_id",
              columnNames = {"child_id", "parent_id"}))
  @JsonIgnoreProperties(
      value = {"club", "parents", "children"},
      allowSetters = true)
  @Builder.Default
  private Set<User> parents = new HashSet<>();

  @ManyToMany(fetch = FetchType.LAZY, mappedBy = "parents")
  @JsonIgnoreProperties(
      value = {"club", "parents", "children"},
      allowSetters = true)
  @Builder.Default
  private Set<User> children = new HashSet<>();

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
