package ee.finalthesis.clubmanagement.security;

import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class SpringSecurityAuditorAware implements AuditorAware<String> {

  private static final String SYSTEM = "system";

  @Override
  @NonNull public Optional<String> getCurrentAuditor() {
    return Optional.of(SecurityUtils.getCurrentUserEmail().orElse(SYSTEM));
  }
}
