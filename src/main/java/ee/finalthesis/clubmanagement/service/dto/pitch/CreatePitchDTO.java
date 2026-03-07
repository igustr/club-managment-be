package ee.finalthesis.clubmanagement.service.dto.pitch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePitchDTO {

  @NotBlank @Size(max = 255) private String name;

  @Size(max = 500) private String address;

  @Size(max = 100) private String surfaceType;

  private Integer capacity;
}
