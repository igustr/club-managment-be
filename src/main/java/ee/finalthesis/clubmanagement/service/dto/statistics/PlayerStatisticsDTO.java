package ee.finalthesis.clubmanagement.service.dto.statistics;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlayerStatisticsDTO {

  private UUID userId;
  private String firstName;
  private String lastName;
  private int totalTrainings;
  private int confirmedCount;
  private int declinedCount;
  private int pendingCount;
  private double attendanceRate;
}
