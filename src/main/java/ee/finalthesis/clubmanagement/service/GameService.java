package ee.finalthesis.clubmanagement.service;

import ee.finalthesis.clubmanagement.common.exception.BadRequestException;
import ee.finalthesis.clubmanagement.common.exception.ResourceNotFoundException;
import ee.finalthesis.clubmanagement.domain.Club;
import ee.finalthesis.clubmanagement.domain.Game;
import ee.finalthesis.clubmanagement.domain.Pitch;
import ee.finalthesis.clubmanagement.domain.Team;
import ee.finalthesis.clubmanagement.domain.TeamMember;
import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.domain.enumeration.GameStatus;
import ee.finalthesis.clubmanagement.domain.enumeration.NotificationType;
import ee.finalthesis.clubmanagement.domain.enumeration.SystemRole;
import ee.finalthesis.clubmanagement.domain.enumeration.VenueType;
import ee.finalthesis.clubmanagement.repository.ClubRepository;
import ee.finalthesis.clubmanagement.repository.GameRepository;
import ee.finalthesis.clubmanagement.repository.GameSquadMemberRepository;
import ee.finalthesis.clubmanagement.repository.PitchRepository;
import ee.finalthesis.clubmanagement.repository.TeamMemberRepository;
import ee.finalthesis.clubmanagement.repository.TeamRepository;
import ee.finalthesis.clubmanagement.security.SecurityUtils;
import ee.finalthesis.clubmanagement.service.dto.game.CreateGameDTO;
import ee.finalthesis.clubmanagement.service.dto.game.GameDTO;
import ee.finalthesis.clubmanagement.service.dto.game.UpdateGameDTO;
import ee.finalthesis.clubmanagement.service.mapper.GameMapper;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameService {

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

  private final GameRepository gameRepository;
  private final TeamRepository teamRepository;
  private final ClubRepository clubRepository;
  private final PitchRepository pitchRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final GameSquadMemberRepository gameSquadMemberRepository;
  private final GameMapper gameMapper;
  private final PitchConflictValidator pitchConflictValidator;
  private final NotificationService notificationService;
  private final MessageSource messageSource;

  @Transactional(readOnly = true)
  public List<GameDTO> listGamesByClub(UUID clubId, boolean myTeams) {
    ClubRole role = SecurityUtils.getCurrentUserRole().orElse(null);
    UUID userId = SecurityUtils.getCurrentUserId().orElse(null);

    if (!myTeams
        && (role == ClubRole.CLUB_ADMIN
            || SecurityUtils.getCurrentUserSystemRole().orElse(null) == SystemRole.MASTER_ADMIN)) {
      return gameMapper.toDto(gameRepository.findByClubIdWithTeam(clubId));
    }

    List<UUID> teamIds =
        teamMemberRepository.findByUserId(userId).stream()
            .filter(tm -> tm.getTeam().getClub().getId().equals(clubId))
            .map(tm -> tm.getTeam().getId())
            .collect(Collectors.toList());

    if (teamIds.isEmpty()) {
      // Still check if user is in any squad
      List<UUID> squadGameIds =
          gameSquadMemberRepository.findGameIdsByUserIdAndClubId(userId, clubId);
      if (squadGameIds.isEmpty()) {
        return List.of();
      }
      return gameMapper.toDto(gameRepository.findAllById(squadGameIds));
    }

    List<Game> teamGames = gameRepository.findByTeamIdInWithTeam(teamIds);

    // Also include games where user is a squad member (cross-team)
    List<UUID> squadGameIds =
        gameSquadMemberRepository.findGameIdsByUserIdAndClubId(userId, clubId);
    Set<UUID> teamGameIds = teamGames.stream().map(Game::getId).collect(Collectors.toSet());
    List<UUID> extraGameIds =
        squadGameIds.stream().filter(id -> !teamGameIds.contains(id)).collect(Collectors.toList());

    if (!extraGameIds.isEmpty()) {
      List<Game> allGames = new ArrayList<>(teamGames);
      allGames.addAll(gameRepository.findAllById(extraGameIds));
      return gameMapper.toDto(allGames);
    }

    return gameMapper.toDto(teamGames);
  }

  @Transactional(readOnly = true)
  public GameDTO getGame(UUID clubId, UUID gameId) {
    Game game =
        gameRepository
            .findByIdAndClubIdWithTeamAndPitch(gameId, clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Game", "id", gameId));
    return gameMapper.toDto(game);
  }

  @Transactional
  public GameDTO createGame(UUID clubId, UUID teamId, CreateGameDTO request) {
    Team team =
        teamRepository
            .findByIdAndClubId(teamId, clubId)
            .orElseThrow(
                () ->
                    new BadRequestException(
                        msg("error.game.teamNotInClub"), "game", "teamNotInClub"));

    Club club =
        clubRepository
            .findById(clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Club", "id", clubId));

    validateTimeRange(request.getStartTime(), request.getEndTime());

    Pitch pitch = resolveVenue(clubId, request.getVenueType(), request.getPitchId());

    if (pitch != null) {
      pitchConflictValidator.checkConflict(
          pitch.getId(),
          request.getDate(),
          request.getStartTime(),
          request.getEndTime(),
          BigDecimal.ONE,
          null,
          null);
    }

    Game game =
        Game.builder()
            .date(request.getDate())
            .gatheringTime(request.getGatheringTime())
            .startTime(request.getStartTime())
            .endTime(request.getEndTime())
            .opponent(request.getOpponent())
            .venueType(request.getVenueType())
            .pitch(pitch)
            .venueName(request.getVenueType() == VenueType.AWAY ? request.getVenueName() : null)
            .venueAddress(
                request.getVenueType() == VenueType.AWAY ? request.getVenueAddress() : null)
            .notes(request.getNotes())
            .team(team)
            .club(club)
            .build();

    game = gameRepository.save(game);

    notifyTeamMembers(game, NotificationType.GAME_CREATED);

    return gameMapper.toDto(game);
  }

  @Transactional
  public GameDTO updateGame(UUID clubId, UUID gameId, UpdateGameDTO request) {
    Game game =
        gameRepository
            .findByIdAndClubId(gameId, clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Game", "id", gameId));

    validateTimeRange(request.getStartTime(), request.getEndTime());

    Pitch pitch = resolveVenue(clubId, request.getVenueType(), request.getPitchId());

    if (pitch != null) {
      pitchConflictValidator.checkConflict(
          pitch.getId(),
          request.getDate(),
          request.getStartTime(),
          request.getEndTime(),
          BigDecimal.ONE,
          null,
          game.getId());
    }

    game.setDate(request.getDate());
    game.setGatheringTime(request.getGatheringTime());
    game.setStartTime(request.getStartTime());
    game.setEndTime(request.getEndTime());
    game.setOpponent(request.getOpponent());
    game.setVenueType(request.getVenueType());
    game.setPitch(pitch);
    game.setVenueName(request.getVenueType() == VenueType.AWAY ? request.getVenueName() : null);
    game.setVenueAddress(
        request.getVenueType() == VenueType.AWAY ? request.getVenueAddress() : null);
    game.setNotes(request.getNotes());

    game = gameRepository.save(game);

    notifyTeamMembers(game, NotificationType.GAME_UPDATED);

    return gameMapper.toDto(game);
  }

  @Transactional
  public void cancelGame(UUID clubId, UUID gameId) {
    Game game =
        gameRepository
            .findByIdAndClubId(gameId, clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Game", "id", gameId));

    if (game.getStatus() == GameStatus.CANCELLED) {
      throw new BadRequestException(msg("error.game.alreadyCancelled"), "game", "alreadyCancelled");
    }

    game.setStatus(GameStatus.CANCELLED);
    gameRepository.save(game);

    notifyTeamMembers(game, NotificationType.GAME_CANCELLED);
  }

  @Transactional
  public void deleteGame(UUID clubId, UUID gameId) {
    Game game =
        gameRepository
            .findByIdAndClubId(gameId, clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Game", "id", gameId));
    gameSquadMemberRepository.deleteByGameId(gameId);
    gameRepository.delete(game);
  }

  private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
    if (!endTime.isAfter(startTime)) {
      throw new BadRequestException(msg("error.game.endBeforeStart"), "game", "endBeforeStart");
    }
  }

  private Pitch resolveVenue(UUID clubId, VenueType venueType, UUID pitchId) {
    if (venueType == VenueType.HOME) {
      if (pitchId == null) {
        throw new BadRequestException(
            msg("error.game.pitchRequiredForHome"), "game", "pitchRequiredForHome");
      }
      return pitchRepository
          .findByIdAndClubId(pitchId, clubId)
          .orElseThrow(() -> new ResourceNotFoundException("Pitch", "id", pitchId));
    }
    return null;
  }

  private void notifyTeamMembers(Game game, NotificationType type) {
    String title = game.getTeam().getName() + " – " + game.getDate().format(DATE_FMT);
    Set<User> recipients = getTeamRecipients(game.getTeam().getId());
    notificationService.notifyUsers(recipients, game.getClub(), type, title, null, game.getId());
  }

  private Set<User> getTeamRecipients(UUID teamId) {
    List<TeamMember> members = teamMemberRepository.findByTeamIdWithUsersAndParents(teamId);
    Set<User> recipients = new HashSet<>();
    for (TeamMember tm : members) {
      recipients.add(tm.getUser());
      if (tm.getUser().getParents() != null) {
        for (User parent : tm.getUser().getParents()) {
          if (parent.getClub() != null
              && parent.getClub().getId().equals(tm.getTeam().getClub().getId())) {
            recipients.add(parent);
          }
        }
      }
    }
    return recipients;
  }

  private String msg(String key, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(key, args, key, locale);
  }
}
