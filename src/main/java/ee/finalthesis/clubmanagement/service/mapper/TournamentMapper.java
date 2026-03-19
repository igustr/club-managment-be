package ee.finalthesis.clubmanagement.service.mapper;

import ee.finalthesis.clubmanagement.domain.Tournament;
import ee.finalthesis.clubmanagement.domain.TournamentSquadMember;
import ee.finalthesis.clubmanagement.service.dto.squad.SquadMemberDTO;
import ee.finalthesis.clubmanagement.service.dto.tournament.TournamentDTO;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TournamentMapper {

  @Mapping(source = "team.id", target = "teamId")
  @Mapping(source = "team.name", target = "teamName")
  @Mapping(source = "pitch.id", target = "pitchId")
  @Mapping(source = "pitch.name", target = "pitchName")
  @Mapping(source = "club.id", target = "clubId")
  TournamentDTO toDto(Tournament tournament);

  List<TournamentDTO> toDto(List<Tournament> tournaments);

  @Mapping(source = "user.id", target = "userId")
  @Mapping(source = "user.firstName", target = "firstName")
  @Mapping(source = "user.lastName", target = "lastName")
  @Mapping(source = "user.email", target = "email")
  @Mapping(source = "user.role", target = "role")
  @Mapping(source = "user.position", target = "position")
  SquadMemberDTO toSquadMemberDto(TournamentSquadMember squadMember);

  List<SquadMemberDTO> toSquadMemberDto(List<TournamentSquadMember> squadMembers);
}
