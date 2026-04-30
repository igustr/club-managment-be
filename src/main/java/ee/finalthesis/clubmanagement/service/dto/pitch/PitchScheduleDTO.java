package ee.finalthesis.clubmanagement.service.dto.pitch;

import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PitchScheduleDTO {

  private LocalDate from;
  private LocalDate to;
  private List<PitchScheduleEntryDTO> pitches;
  private List<PitchConflictDTO> conflicts;
}
