package ee.finalthesis.clubmanagement.service.dto.pitch;

import ee.finalthesis.clubmanagement.domain.enumeration.SurfaceType;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PitchDTO {

  private UUID id;
  private String name;
  private String address;
  private SurfaceType surfaceType;
  private UUID clubId;
}
