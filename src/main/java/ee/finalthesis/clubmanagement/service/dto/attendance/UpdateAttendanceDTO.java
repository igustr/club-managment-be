package ee.finalthesis.clubmanagement.service.dto.attendance;

import ee.finalthesis.clubmanagement.domain.enumeration.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAttendanceDTO {

  @NotNull private AttendanceStatus status;
}
