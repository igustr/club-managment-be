package ee.finalthesis.clubmanagement.service.dto.training;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateTrainingSessionDTO {

  @NotNull private LocalDate date;

  @NotNull private LocalTime startTime;

  @NotNull private LocalTime endTime;

  private UUID pitchId;

  private BigDecimal pitchPortion;

  @Size(max = 1000) private String notes;
}
