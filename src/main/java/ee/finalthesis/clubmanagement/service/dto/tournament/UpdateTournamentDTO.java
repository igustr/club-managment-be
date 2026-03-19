package ee.finalthesis.clubmanagement.service.dto.tournament;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateTournamentDTO {

  @NotBlank @Size(max = 255) private String name;

  @NotNull private LocalDate startDate;

  @NotNull private LocalDate endDate;

  private UUID pitchId;

  @Size(max = 255) private String venueName;

  @Size(max = 500) private String venueAddress;

  @Size(max = 1000) private String notes;
}
