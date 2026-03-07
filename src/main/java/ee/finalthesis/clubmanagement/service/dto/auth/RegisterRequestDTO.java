package ee.finalthesis.clubmanagement.service.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequestDTO {

  @NotBlank @Email @Size(max = 255) private String email;

  @NotBlank @Size(min = 8, max = 100) private String password;

  @NotBlank @Size(max = 100) private String firstName;

  @NotBlank @Size(max = 100) private String lastName;

  @NotNull private LocalDate dateOfBirth;

  @NotBlank @Size(max = 50) private String phone;
}
