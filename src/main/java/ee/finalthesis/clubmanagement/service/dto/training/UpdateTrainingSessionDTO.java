package ee.finalthesis.clubmanagement.service.dto.training;

import ee.finalthesis.clubmanagement.domain.enumeration.TrainingSessionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTrainingSessionDTO {

  @NotNull private LocalDate date;

  @NotNull private LocalTime startTime;

  @NotNull private LocalTime endTime;

  private UUID pitchId;

  @Size(max = 1000) private String notes;

  private TrainingSessionStatus status;
}
