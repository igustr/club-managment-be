package ee.finalthesis.clubmanagement.service.dto.tournament;

import ee.finalthesis.clubmanagement.domain.enumeration.TournamentStatus;
import ee.finalthesis.clubmanagement.domain.enumeration.VenueType;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TournamentDTO {

  private UUID id;
  private String name;
  private LocalDate startDate;
  private LocalDate endDate;
  private VenueType venueType;
  private UUID pitchId;
  private String pitchName;
  private String venueName;
  private String venueAddress;
  private TournamentStatus status;
  private String notes;
  private UUID teamId;
  private String teamName;
  private UUID clubId;
}
