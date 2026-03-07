package ee.finalthesis.clubmanagement.service.dto.attendance;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AttendanceSummaryDTO {

  private int total;
  private int confirmed;
  private int declined;
  private int pending;
  private List<AttendanceDTO> attendances;
}
