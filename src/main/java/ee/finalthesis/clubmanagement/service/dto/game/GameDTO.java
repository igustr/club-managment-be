package ee.finalthesis.clubmanagement.service.dto.game;

import ee.finalthesis.clubmanagement.domain.enumeration.GameStatus;
import ee.finalthesis.clubmanagement.domain.enumeration.VenueType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameDTO {

  private UUID id;
  private LocalDate date;
  private LocalTime startTime;
  private LocalTime endTime;
  private String opponent;
  private VenueType venueType;
  private UUID pitchId;
  private String pitchName;
  private String venueName;
  private String venueAddress;
  private GameStatus status;
  private String notes;
  private UUID teamId;
  private String teamName;
  private UUID clubId;
}
