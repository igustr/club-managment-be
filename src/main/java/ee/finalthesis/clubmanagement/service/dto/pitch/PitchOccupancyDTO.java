package ee.finalthesis.clubmanagement.service.dto.pitch;

import ee.finalthesis.clubmanagement.service.dto.training.TrainingSessionDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PitchOccupancyDTO {

  private UUID pitchId;
  private String pitchName;
  private LocalDate date;
  private BigDecimal totalOccupancy;
  private List<TrainingSessionDTO> sessions;
}
