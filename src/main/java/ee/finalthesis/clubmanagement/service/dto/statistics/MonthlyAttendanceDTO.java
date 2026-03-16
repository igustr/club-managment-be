package ee.finalthesis.clubmanagement.service.dto.statistics;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MonthlyAttendanceDTO {

  private String month;
  private int totalTrainings;
  private int totalAttendances;
  private int confirmedCount;
  private double attendanceRate;
}
