package ee.finalthesis.clubmanagement.service.dto.auth;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponseDTO {

  private final String accessToken;
  private final String refreshToken;

  @Builder.Default private final String tokenType = "Bearer";

  private final long expiresIn;
}
