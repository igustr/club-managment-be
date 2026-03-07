package ee.finalthesis.clubmanagement.service.dto.club;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateClubDTO {

  @NotBlank @Size(max = 255) private String name;

  @Size(max = 50) private String registrationCode;

  @Size(max = 500) private String address;

  @Size(max = 255) private String contactEmail;

  @Size(max = 50) private String contactPhone;
}
