package ee.finalthesis.clubmanagement.service;

import ee.finalthesis.clubmanagement.common.exception.BadRequestException;
import ee.finalthesis.clubmanagement.common.exception.ConflictException;
import ee.finalthesis.clubmanagement.common.exception.ResourceNotFoundException;
import ee.finalthesis.clubmanagement.domain.Club;
import ee.finalthesis.clubmanagement.domain.Team;
import ee.finalthesis.clubmanagement.domain.TeamMember;
import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.domain.enumeration.SystemRole;
import ee.finalthesis.clubmanagement.repository.ClubRepository;
import ee.finalthesis.clubmanagement.repository.TeamMemberRepository;
import ee.finalthesis.clubmanagement.repository.TeamRepository;
import ee.finalthesis.clubmanagement.repository.UserRepository;
import ee.finalthesis.clubmanagement.security.SecurityUtils;
import ee.finalthesis.clubmanagement.service.dto.team.AddTeamMemberDTO;
import ee.finalthesis.clubmanagement.service.dto.team.CreateTeamDTO;
import ee.finalthesis.clubmanagement.service.dto.team.TeamDTO;
import ee.finalthesis.clubmanagement.service.dto.team.TeamMemberDTO;
import ee.finalthesis.clubmanagement.service.dto.team.UpdateTeamDTO;
import ee.finalthesis.clubmanagement.service.mapper.TeamMapper;
import ee.finalthesis.clubmanagement.service.mapper.TeamMemberMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamService {

  private final TeamRepository teamRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final ClubRepository clubRepository;
  private final UserRepository userRepository;
  private final TeamMapper teamMapper;
  private final TeamMemberMapper teamMemberMapper;
  private final ConversationService conversationService;
  private final MessageSource messageSource;

  @Transactional(readOnly = true)
  public List<TeamDTO> listTeams(UUID clubId) {
    ClubRole role = SecurityUtils.getCurrentUserRole().orElse(null);
    UUID userId = SecurityUtils.getCurrentUserId().orElse(null);

    if (role == ClubRole.CLUB_ADMIN
        || SecurityUtils.getCurrentUserSystemRole().orElse(null) == SystemRole.MASTER_ADMIN) {
      List<Team> teams = teamRepository.findByClubId(clubId);
      return teamMapper.toDto(teams);
    }

    List<TeamMember> memberships = teamMemberRepository.findByUserId(userId);
    List<Team> teams =
        memberships.stream()
            .map(TeamMember::getTeam)
            .filter(team -> team.getClub().getId().equals(clubId))
            .collect(Collectors.toList());
    return teamMapper.toDto(teams);
  }

  @Transactional
  public TeamDTO createTeam(UUID clubId, CreateTeamDTO request) {
    Club club =
        clubRepository
            .findById(clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Club", "id", clubId));

    Team team =
        Team.builder()
            .name(request.getName())
            .ageGroup(request.getAgeGroup())
            .season(request.getSeason())
            .club(club)
            .build();

    team = teamRepository.save(team);
    conversationService.createTeamConversation(team);
    return teamMapper.toDto(team);
  }

  @Transactional(readOnly = true)
  public TeamDTO getTeam(UUID clubId, UUID teamId) {
    Team team =
        teamRepository
            .findByIdAndClubId(teamId, clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));
    return teamMapper.toDto(team);
  }

  @Transactional
  public TeamDTO updateTeam(UUID clubId, UUID teamId, UpdateTeamDTO request) {
    Team team =
        teamRepository
            .findByIdAndClubId(teamId, clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

    team.setName(request.getName());
    team.setAgeGroup(request.getAgeGroup());
    team.setSeason(request.getSeason());

    team = teamRepository.save(team);
    return teamMapper.toDto(team);
  }

  @Transactional
  public void deleteTeam(UUID clubId, UUID teamId) {
    Team team =
        teamRepository
            .findByIdAndClubId(teamId, clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));
    teamRepository.delete(team);
  }

  @Transactional(readOnly = true)
  public List<TeamMemberDTO> listTeamMembers(UUID clubId, UUID teamId) {
    if (!teamRepository.existsByIdAndClubId(teamId, clubId)) {
      throw new ResourceNotFoundException("Team", "id", teamId);
    }
    List<TeamMember> members = teamMemberRepository.findByTeamId(teamId);
    return teamMemberMapper.toDto(members);
  }

  @Transactional
  public TeamMemberDTO addTeamMember(UUID clubId, UUID teamId, AddTeamMemberDTO request) {
    Team team =
        teamRepository
            .findByIdAndClubId(teamId, clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

    User user =
        userRepository
            .findByIdAndClubId(request.getUserId(), clubId)
            .orElseThrow(
                () ->
                    new BadRequestException(
                        msg("error.teamMember.userNotInClub"), "teamMember", "userNotInClub"));

    if (teamMemberRepository.existsByTeamIdAndUserId(teamId, request.getUserId())) {
      throw new ConflictException(
          msg("error.teamMember.alreadyExists"), "teamMember", "alreadyExists");
    }

    TeamMember teamMember =
        TeamMember.builder().team(team).user(user).joinedDate(LocalDate.now()).build();

    teamMember = teamMemberRepository.save(teamMember);
    conversationService.addParticipantToTeamConversation(teamId, user);
    return teamMemberMapper.toDto(teamMember);
  }

  @Transactional
  public void removeTeamMember(UUID clubId, UUID teamId, UUID userId) {
    if (!teamRepository.existsByIdAndClubId(teamId, clubId)) {
      throw new ResourceNotFoundException("Team", "id", teamId);
    }

    if (!teamMemberRepository.existsByTeamIdAndUserId(teamId, userId)) {
      throw new ResourceNotFoundException(msg("error.teamMember.notFound"), "teamMember", userId);
    }

    conversationService.removeParticipantFromTeamConversation(teamId, userId);
    teamMemberRepository.deleteByTeamIdAndUserId(teamId, userId);
  }

  private String msg(String key, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(key, args, key, locale);
  }
}
