package ee.finalthesis.clubmanagement.service.dto.pitch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
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
public class PitchConflictDTO {

  private UUID pitchId;
  private String pitchName;
  private LocalDate date;
  private LocalTime overlapStart;
  private LocalTime overlapEnd;
  private BigDecimal totalOccupancy;
  private List<UUID> eventIds;
}
