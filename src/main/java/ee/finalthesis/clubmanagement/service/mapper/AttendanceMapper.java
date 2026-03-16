package ee.finalthesis.clubmanagement.service.mapper;

import ee.finalthesis.clubmanagement.domain.Attendance;
import ee.finalthesis.clubmanagement.service.dto.attendance.AttendanceDTO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AttendanceMapper {

  @Mapping(source = "trainingSession.id", target = "trainingSessionId")
  @Mapping(source = "user.id", target = "userId")
  @Mapping(source = "user.firstName", target = "firstName")
  @Mapping(source = "user.lastName", target = "lastName")
  @Mapping(source = "user.email", target = "email")
  @Mapping(source = "user.role", target = "role")
  @Mapping(source = "user.position", target = "position")
  AttendanceDTO toDto(Attendance attendance);

  List<AttendanceDTO> toDto(List<Attendance> attendances);
}
