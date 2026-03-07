package ee.finalthesis.clubmanagement.service.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenRequestDTO {

  @NotBlank private String refreshToken;
}
