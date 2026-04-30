package ee.finalthesis.clubmanagement.service.dto.pitch;

import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PitchScheduleEntryDTO {

  private UUID pitchId;
  private String pitchName;
  private List<PitchScheduleEventDTO> events;
}
