package ee.finalthesis.clubmanagement.service.dto.training;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRecurringTrainingDTO {

  @NotNull private LocalDate startDate;

  @NotNull private LocalDate endDate;

  @NotNull private DayOfWeek dayOfWeek;

  @NotNull private LocalTime startTime;

  @NotNull private LocalTime endTime;

  private UUID pitchId;

  @Size(max = 1000) private String notes;
}
