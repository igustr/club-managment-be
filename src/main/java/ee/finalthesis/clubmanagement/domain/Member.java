package ee.finalthesis.clubmanagement.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ee.finalthesis.clubmanagement.domain.enumeration.MemberStatus;
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
@Table(name = "member")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Member extends AbstractAuditingEntity<UUID> implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @NotBlank @Size(max = 100) @Column(name = "first_name", length = 100, nullable = false)
  private String firstName;

  @NotBlank @Size(max = 100) @Column(name = "last_name", length = 100, nullable = false)
  private String lastName;

  @Column(name = "date_of_birth")
  private LocalDate dateOfBirth;

  @Size(max = 255) @Column(name = "email")
  private String email;

  @Size(max = 50) @Column(name = "phone", length = 50)
  private String phone;

  @NotNull @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  @Builder.Default
  private MemberStatus status = MemberStatus.ACTIVE;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  @JsonIgnoreProperties(
      value = {"club"},
      allowSetters = true)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "club_id", nullable = false)
  @JsonIgnoreProperties(
      value = {"users", "teams", "members", "fields"},
      allowSetters = true)
  private Club club;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  @JsonIgnoreProperties(
      value = {"club"},
      allowSetters = true)
  private User parent;

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "member")
  @JsonIgnoreProperties(
      value = {"member"},
      allowSetters = true)
  @Builder.Default
  private Set<TeamMember> teamMembers = new HashSet<>();

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Member)) return false;
    return getId() != null && getId().equals(((Member) o).getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
