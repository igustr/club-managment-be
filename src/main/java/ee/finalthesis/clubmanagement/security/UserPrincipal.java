package ee.finalthesis.clubmanagement.security;

import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class UserPrincipal implements UserDetails {

  private final UUID id;
  private final String email;
  private final String password;
  private final ClubRole role;
  private final UUID clubId;
  private final boolean active;
  private final Collection<? extends GrantedAuthority> authorities;

  private UserPrincipal(
      UUID id,
      String email,
      String password,
      ClubRole role,
      UUID clubId,
      boolean active,
      Collection<? extends GrantedAuthority> authorities) {
    this.id = id;
    this.email = email;
    this.password = password;
    this.role = role;
    this.clubId = clubId;
    this.active = active;
    this.authorities = authorities;
  }

  public static UserPrincipal create(User user) {
    List<GrantedAuthority> authorities =
        user.getRole() != null
            ? List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            : List.of();

    return new UserPrincipal(
        user.getId(),
        user.getEmail(),
        user.getPasswordHash(),
        user.getRole(),
        user.getClub() != null ? user.getClub().getId() : null,
        user.getActive(),
        authorities);
  }

  public static UserPrincipal fromToken(UUID id, String email, ClubRole role, UUID clubId) {
    List<GrantedAuthority> authorities =
        role != null ? List.of(new SimpleGrantedAuthority("ROLE_" + role.name())) : List.of();

    return new UserPrincipal(id, email, null, role, clubId, true, authorities);
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isEnabled() {
    return active;
  }
}
