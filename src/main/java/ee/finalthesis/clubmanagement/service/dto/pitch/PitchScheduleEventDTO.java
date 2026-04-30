package ee.finalthesis.clubmanagement.service.dto.pitch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PitchScheduleEventDTO {

  public enum EventType {
    TRAINING,
    GAME
  }

  private UUID id;
  private EventType eventType;
  private LocalDate date;
  private LocalTime startTime;
  private LocalTime endTime;
  private UUID teamId;
  private String teamName;
  private BigDecimal pitchPortion;
  private String label;
}
