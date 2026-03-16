package ee.finalthesis.clubmanagement.service.mapper;

import ee.finalthesis.clubmanagement.domain.TeamMember;
import ee.finalthesis.clubmanagement.service.dto.team.TeamMemberDTO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TeamMemberMapper {

  @Mapping(source = "user.id", target = "userId")
  @Mapping(source = "team.id", target = "teamId")
  @Mapping(source = "user.firstName", target = "firstName")
  @Mapping(source = "user.lastName", target = "lastName")
  @Mapping(source = "user.email", target = "email")
  @Mapping(source = "user.role", target = "role")
  @Mapping(source = "user.position", target = "position")
  TeamMemberDTO toDto(TeamMember teamMember);

  List<TeamMemberDTO> toDto(List<TeamMember> teamMembers);
}
