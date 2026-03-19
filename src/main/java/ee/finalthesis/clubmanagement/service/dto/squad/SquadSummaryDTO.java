package ee.finalthesis.clubmanagement.service.dto.squad;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SquadSummaryDTO {

  private int total;
  private int confirmed;
  private int declined;
  private int pending;
  private List<SquadMemberDTO> members;
}
