package ee.finalthesis.clubmanagement.service.dto.statistics;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TeamStatisticsDTO {

  private UUID teamId;
  private String teamName;
  private int memberCount;
  private int totalTrainings;
  private double averageAttendanceRate;
  private List<PlayerStatisticsDTO> playerStatistics;
}
