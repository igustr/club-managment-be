package ee.finalthesis.clubmanagement.service.mapper;

import ee.finalthesis.clubmanagement.domain.Game;
import ee.finalthesis.clubmanagement.domain.GameSquadMember;
import ee.finalthesis.clubmanagement.service.dto.game.GameDTO;
import ee.finalthesis.clubmanagement.service.dto.squad.SquadMemberDTO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface GameMapper {

  @Mapping(source = "team.id", target = "teamId")
  @Mapping(source = "team.name", target = "teamName")
  @Mapping(source = "pitch.id", target = "pitchId")
  @Mapping(source = "pitch.name", target = "pitchName")
  @Mapping(source = "club.id", target = "clubId")
  GameDTO toDto(Game game);

  List<GameDTO> toDto(List<Game> games);

  @Mapping(source = "user.id", target = "userId")
  @Mapping(source = "user.firstName", target = "firstName")
  @Mapping(source = "user.lastName", target = "lastName")
  @Mapping(source = "user.email", target = "email")
  @Mapping(source = "user.role", target = "role")
  @Mapping(source = "user.position", target = "position")
  SquadMemberDTO toSquadMemberDto(GameSquadMember squadMember);

  List<SquadMemberDTO> toSquadMemberDto(List<GameSquadMember> squadMembers);
}
