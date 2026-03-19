package ee.finalthesis.clubmanagement.service;

import ee.finalthesis.clubmanagement.common.exception.BadRequestException;
import ee.finalthesis.clubmanagement.common.exception.ResourceNotFoundException;
import ee.finalthesis.clubmanagement.domain.Club;
import ee.finalthesis.clubmanagement.domain.Pitch;
import ee.finalthesis.clubmanagement.domain.Team;
import ee.finalthesis.clubmanagement.domain.Tournament;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.domain.enumeration.SystemRole;
import ee.finalthesis.clubmanagement.domain.enumeration.TournamentStatus;
import ee.finalthesis.clubmanagement.domain.enumeration.VenueType;
import ee.finalthesis.clubmanagement.repository.ClubRepository;
import ee.finalthesis.clubmanagement.repository.PitchRepository;
import ee.finalthesis.clubmanagement.repository.TeamMemberRepository;
import ee.finalthesis.clubmanagement.repository.TeamRepository;
import ee.finalthesis.clubmanagement.repository.TournamentRepository;
import ee.finalthesis.clubmanagement.repository.TournamentSquadMemberRepository;
import ee.finalthesis.clubmanagement.security.SecurityUtils;
import ee.finalthesis.clubmanagement.service.dto.tournament.CreateTournamentDTO;
import ee.finalthesis.clubmanagement.service.dto.tournament.TournamentDTO;
import ee.finalthesis.clubmanagement.service.dto.tournament.UpdateTournamentDTO;
import ee.finalthesis.clubmanagement.service.mapper.TournamentMapper;
import java.util.ArrayList;
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
public class TournamentService {

  private final TournamentRepository tournamentRepository;
  private final TeamRepository teamRepository;
  private final ClubRepository clubRepository;
  private final PitchRepository pitchRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final TournamentSquadMemberRepository tournamentSquadMemberRepository;
  private final TournamentMapper tournamentMapper;
  private final MessageSource messageSource;

  @Transactional(readOnly = true)
  public List<TournamentDTO> listTournamentsByClub(UUID clubId, boolean myTeams) {
    ClubRole role = SecurityUtils.getCurrentUserRole().orElse(null);
    UUID userId = SecurityUtils.getCurrentUserId().orElse(null);

    if (!myTeams
        && (role == ClubRole.CLUB_ADMIN
            || SecurityUtils.getCurrentUserSystemRole().orElse(null) == SystemRole.MASTER_ADMIN)) {
      return tournamentMapper.toDto(tournamentRepository.findByClubIdWithTeam(clubId));
    }

    List<UUID> teamIds =
        teamMemberRepository.findByUserId(userId).stream()
            .filter(tm -> tm.getTeam().getClub().getId().equals(clubId))
            .map(tm -> tm.getTeam().getId())
            .collect(Collectors.toList());

    if (teamIds.isEmpty()) {
      List<UUID> squadTournamentIds =
          tournamentSquadMemberRepository.findTournamentIdsByUserIdAndClubId(userId, clubId);
      if (squadTournamentIds.isEmpty()) {
        return List.of();
      }
      return tournamentMapper.toDto(tournamentRepository.findAllById(squadTournamentIds));
    }

    List<Tournament> teamTournaments = tournamentRepository.findByTeamIdInWithTeam(teamIds);

    // Also include tournaments where user is a squad member (cross-team)
    List<UUID> squadTournamentIds =
        tournamentSquadMemberRepository.findTournamentIdsByUserIdAndClubId(userId, clubId);
    Set<UUID> teamTournamentIds =
        teamTournaments.stream().map(Tournament::getId).collect(Collectors.toSet());
    List<UUID> extraIds =
        squadTournamentIds.stream()
            .filter(id -> !teamTournamentIds.contains(id))
            .collect(Collectors.toList());

    if (!extraIds.isEmpty()) {
      List<Tournament> allTournaments = new ArrayList<>(teamTournaments);
      allTournaments.addAll(tournamentRepository.findAllById(extraIds));
      return tournamentMapper.toDto(allTournaments);
    }

    return tournamentMapper.toDto(teamTournaments);
  }

  @Transactional(readOnly = true)
  public TournamentDTO getTournament(UUID clubId, UUID tournamentId) {
    Tournament tournament =
        tournamentRepository
            .findByIdAndClubIdWithTeamAndPitch(tournamentId, clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Tournament", "id", tournamentId));
    return tournamentMapper.toDto(tournament);
  }

  @Transactional
  public TournamentDTO createTournament(UUID clubId, UUID teamId, CreateTournamentDTO request) {
    Team team =
        teamRepository
            .findByIdAndClubId(teamId, clubId)
            .orElseThrow(
                () ->
                    new BadRequestException(
                        msg("error.tournament.teamNotInClub"), "tournament", "teamNotInClub"));

    Club club =
        clubRepository
            .findById(clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Club", "id", clubId));

    validateDateRange(request);

    Pitch pitch = resolveVenue(clubId, request.getVenueType(), request.getPitchId());

    Tournament tournament =
        Tournament.builder()
            .name(request.getName())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .venueType(request.getVenueType())
            .pitch(pitch)
            .venueName(request.getVenueType() == VenueType.AWAY ? request.getVenueName() : null)
            .venueAddress(
                request.getVenueType() == VenueType.AWAY ? request.getVenueAddress() : null)
            .notes(request.getNotes())
            .team(team)
            .club(club)
            .build();

    tournament = tournamentRepository.save(tournament);
    return tournamentMapper.toDto(tournament);
  }

  @Transactional
  public TournamentDTO updateTournament(
      UUID clubId, UUID tournamentId, UpdateTournamentDTO request) {
    Tournament tournament =
        tournamentRepository
            .findByIdAndClubId(tournamentId, clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Tournament", "id", tournamentId));

    validateDateRange(request);

    Pitch pitch = resolveVenue(clubId, request.getVenueType(), request.getPitchId());

    tournament.setName(request.getName());
    tournament.setStartDate(request.getStartDate());
    tournament.setEndDate(request.getEndDate());
    tournament.setVenueType(request.getVenueType());
    tournament.setPitch(pitch);
    tournament.setVenueName(
        request.getVenueType() == VenueType.AWAY ? request.getVenueName() : null);
    tournament.setVenueAddress(
        request.getVenueType() == VenueType.AWAY ? request.getVenueAddress() : null);
    tournament.setNotes(request.getNotes());

    tournament = tournamentRepository.save(tournament);
    return tournamentMapper.toDto(tournament);
  }

  @Transactional
  public void cancelTournament(UUID clubId, UUID tournamentId) {
    Tournament tournament =
        tournamentRepository
            .findByIdAndClubId(tournamentId, clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Tournament", "id", tournamentId));

    if (tournament.getStatus() == TournamentStatus.CANCELLED) {
      throw new BadRequestException(
          msg("error.tournament.alreadyCancelled"), "tournament", "alreadyCancelled");
    }

    tournament.setStatus(TournamentStatus.CANCELLED);
    tournamentRepository.save(tournament);
  }

  @Transactional
  public void deleteTournament(UUID clubId, UUID tournamentId) {
    Tournament tournament =
        tournamentRepository
            .findByIdAndClubId(tournamentId, clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Tournament", "id", tournamentId));
    tournamentSquadMemberRepository.deleteByTournamentId(tournamentId);
    tournamentRepository.delete(tournament);
  }

  private void validateDateRange(CreateTournamentDTO request) {
    if (request.getEndDate().isBefore(request.getStartDate())) {
      throw new BadRequestException(
          msg("error.tournament.endDateBeforeStart"), "tournament", "endDateBeforeStart");
    }
  }

  private void validateDateRange(UpdateTournamentDTO request) {
    if (request.getEndDate().isBefore(request.getStartDate())) {
      throw new BadRequestException(
          msg("error.tournament.endDateBeforeStart"), "tournament", "endDateBeforeStart");
    }
  }

  private Pitch resolveVenue(UUID clubId, VenueType venueType, UUID pitchId) {
    if (venueType == VenueType.HOME) {
      if (pitchId == null) {
        throw new BadRequestException(
            msg("error.tournament.pitchRequiredForHome"), "tournament", "pitchRequiredForHome");
      }
      return pitchRepository
          .findByIdAndClubId(pitchId, clubId)
          .orElseThrow(() -> new ResourceNotFoundException("Pitch", "id", pitchId));
    }
    return null;
  }

  private String msg(String key, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(key, args, key, locale);
  }
}
