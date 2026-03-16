package ee.finalthesis.clubmanagement.service.dto.statistics;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClubStatisticsDTO {

  private int totalMembers;
  private int totalTeams;
  private int totalTrainings;
  private double overallAttendanceRate;
  private List<TeamStatisticsDTO> teamStatistics;
  private List<MonthlyAttendanceDTO> monthlyAttendance;
}
