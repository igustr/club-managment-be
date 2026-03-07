package ee.finalthesis.clubmanagement.service.dto.pitch;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PitchDTO {

  private UUID id;
  private String name;
  private String address;
  private String surfaceType;
  private Integer capacity;
  private UUID clubId;
}
