package ee.finalthesis.clubmanagement.service.dto.game;

import ee.finalthesis.clubmanagement.domain.enumeration.VenueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateGameDTO {

  @NotNull private LocalDate date;

  @NotNull private LocalTime startTime;

  @NotNull private LocalTime endTime;

  @NotBlank @Size(max = 255) private String opponent;

  @NotNull private VenueType venueType;

  private UUID pitchId;

  @Size(max = 255) private String venueName;

  @Size(max = 500) private String venueAddress;

  @Size(max = 1000) private String notes;
}
