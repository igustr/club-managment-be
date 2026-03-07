package ee.finalthesis.clubmanagement.service.dto.team;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TeamDTO {

  private UUID id;
  private String name;
  private String ageGroup;
  private String season;
  private UUID clubId;
  private int memberCount;
}
