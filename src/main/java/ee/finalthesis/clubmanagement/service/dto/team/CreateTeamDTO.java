package ee.finalthesis.clubmanagement.service.dto.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTeamDTO {

  @NotBlank @Size(max = 255) private String name;

  @Size(max = 50) private String ageGroup;

  @Size(max = 20) private String season;
}
