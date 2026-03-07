package ee.finalthesis.clubmanagement.security;

import ee.finalthesis.clubmanagement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    return userRepository
        .findByEmail(email)
        .map(
            user -> {
              if (!user.getActive()) {
                throw new UsernameNotFoundException("User account is deactivated: " + email);
              }
              return UserPrincipal.create(user);
            })
        .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
  }
}
