package ee.finalthesis.clubmanagement.service.dto.squad;

import ee.finalthesis.clubmanagement.domain.enumeration.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSquadMemberStatusDTO {

  @NotNull private AttendanceStatus status;
}
