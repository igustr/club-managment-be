package ee.finalthesis.clubmanagement.service.mapper;

import ee.finalthesis.clubmanagement.domain.Team;
import ee.finalthesis.clubmanagement.service.dto.team.TeamDTO;
import java.util.List;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TeamMapper {

  @Mapping(source = "club.id", target = "clubId")
  @Mapping(target = "memberCount", ignore = true)
  TeamDTO toDto(Team team);

  List<TeamDTO> toDto(List<Team> teams);

  @AfterMapping
  default void setMemberCount(Team team, @MappingTarget TeamDTO dto) {
    if (team.getTeamMembers() != null) {
      dto.setMemberCount(team.getTeamMembers().size());
    }
  }
}
