package ee.finalthesis.clubmanagement.service.dto.training;

import ee.finalthesis.clubmanagement.domain.enumeration.TrainingSessionStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TrainingSessionDTO {

  private UUID id;
  private LocalDate date;
  private LocalTime startTime;
  private LocalTime endTime;
  private UUID teamId;
  private String teamName;
  private UUID pitchId;
  private String pitchName;
  private TrainingSessionStatus status;
  private String notes;
  private UUID recurrenceGroupId;
}
