package ee.finalthesis.clubmanagement.service;

import ee.finalthesis.clubmanagement.common.exception.BadRequestException;
import ee.finalthesis.clubmanagement.common.exception.ConflictException;
import ee.finalthesis.clubmanagement.common.exception.ResourceNotFoundException;
import ee.finalthesis.clubmanagement.domain.Pitch;
import ee.finalthesis.clubmanagement.domain.Team;
import ee.finalthesis.clubmanagement.domain.TrainingSession;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.domain.enumeration.TrainingSessionStatus;
import ee.finalthesis.clubmanagement.repository.PitchRepository;
import ee.finalthesis.clubmanagement.repository.TeamMemberRepository;
import ee.finalthesis.clubmanagement.repository.TeamRepository;
import ee.finalthesis.clubmanagement.repository.TrainingSessionRepository;
import ee.finalthesis.clubmanagement.security.SecurityUtils;
import ee.finalthesis.clubmanagement.service.dto.training.CreateRecurringTrainingDTO;
import ee.finalthesis.clubmanagement.service.dto.training.CreateTrainingSessionDTO;
import ee.finalthesis.clubmanagement.service.dto.training.TrainingSessionDTO;
import ee.finalthesis.clubmanagement.service.dto.training.UpdateTrainingSessionDTO;
import ee.finalthesis.clubmanagement.service.mapper.TrainingSessionMapper;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
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
public class TrainingSessionService {

  private final TrainingSessionRepository trainingSessionRepository;
  private final TeamRepository teamRepository;
  private final PitchRepository pitchRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final TrainingSessionMapper trainingSessionMapper;
  private final AttendanceService attendanceService;
  private final MessageSource messageSource;

  @Transactional(readOnly = true)
  public List<TrainingSessionDTO> listTrainingsByClub(UUID clubId) {
    ClubRole role = SecurityUtils.getCurrentUserRole().orElse(null);
    UUID userId = SecurityUtils.getCurrentUserId().orElse(null);

    if (role == ClubRole.ADMIN) {
      return trainingSessionMapper.toDto(trainingSessionRepository.findByTeamClubId(clubId));
    }

    // Non-admin: only show trainings for teams the user belongs to
    List<UUID> teamIds =
        teamMemberRepository.findByUserId(userId).stream()
            .filter(tm -> tm.getTeam().getClub().getId().equals(clubId))
            .map(tm -> tm.getTeam().getId())
            .collect(Collectors.toList());

    List<TrainingSession> sessions = new ArrayList<>();
    for (UUID teamId : teamIds) {
      sessions.addAll(trainingSessionRepository.findByTeamId(teamId));
    }
    return trainingSessionMapper.toDto(sessions);
  }

  @Transactional(readOnly = true)
  public TrainingSessionDTO getTraining(UUID clubId, UUID trainingId) {
    TrainingSession training =
        trainingSessionRepository
            .findByIdAndTeamClubId(trainingId, clubId)
            .orElseThrow(() -> new ResourceNotFoundException("TrainingSession", "id", trainingId));
    return trainingSessionMapper.toDto(training);
  }

  @Transactional
  public TrainingSessionDTO createTraining(
      UUID clubId, UUID teamId, CreateTrainingSessionDTO request) {
    Team team =
        teamRepository
            .findByIdAndClubId(teamId, clubId)
            .orElseThrow(
                () ->
                    new BadRequestException(
                        msg("error.training.teamNotInClub"), "trainingSession", "teamNotInClub"));

    validateTimeRange(request.getStartTime(), request.getEndTime());

    Pitch pitch = resolvePitch(clubId, request.getPitchId());

    if (pitch != null) {
      checkPitchConflict(
          pitch.getId(), request.getDate(), request.getStartTime(), request.getEndTime(), null);
    }

    TrainingSession training =
        TrainingSession.builder()
            .date(request.getDate())
            .startTime(request.getStartTime())
            .endTime(request.getEndTime())
            .notes(request.getNotes())
            .team(team)
            .pitch(pitch)
            .build();

    training = trainingSessionRepository.save(training);
    attendanceService.createAttendanceForTraining(training);
    return trainingSessionMapper.toDto(training);
  }

  @Transactional
  public List<TrainingSessionDTO> createRecurringTraining(
      UUID clubId, UUID teamId, CreateRecurringTrainingDTO request) {
    Team team =
        teamRepository
            .findByIdAndClubId(teamId, clubId)
            .orElseThrow(
                () ->
                    new BadRequestException(
                        msg("error.training.teamNotInClub"), "trainingSession", "teamNotInClub"));

    validateTimeRange(request.getStartTime(), request.getEndTime());

    if (!request.getEndDate().isAfter(request.getStartDate())) {
      throw new BadRequestException(
          msg("error.training.endDateBeforeStart"), "trainingSession", "endDateBeforeStart");
    }

    Pitch pitch = resolvePitch(clubId, request.getPitchId());

    UUID recurrenceGroupId = UUID.randomUUID();
    List<TrainingSession> sessions = new ArrayList<>();

    LocalDate current = request.getStartDate();
    // Advance to the first matching day of week
    while (current.getDayOfWeek() != request.getDayOfWeek()) {
      current = current.plusDays(1);
    }

    while (!current.isAfter(request.getEndDate())) {
      if (pitch != null) {
        checkPitchConflict(
            pitch.getId(), current, request.getStartTime(), request.getEndTime(), null);
      }

      TrainingSession training =
          TrainingSession.builder()
              .date(current)
              .startTime(request.getStartTime())
              .endTime(request.getEndTime())
              .notes(request.getNotes())
              .team(team)
              .pitch(pitch)
              .recurrenceGroupId(recurrenceGroupId)
              .build();

      training = trainingSessionRepository.save(training);
      attendanceService.createAttendanceForTraining(training);
      sessions.add(training);
      current = current.plusWeeks(1);
    }

    return trainingSessionMapper.toDto(sessions);
  }

  @Transactional
  public TrainingSessionDTO updateTraining(
      UUID clubId, UUID trainingId, UpdateTrainingSessionDTO request) {
    TrainingSession training =
        trainingSessionRepository
            .findByIdAndTeamClubId(trainingId, clubId)
            .orElseThrow(() -> new ResourceNotFoundException("TrainingSession", "id", trainingId));

    validateTimeRange(request.getStartTime(), request.getEndTime());

    Pitch pitch = resolvePitch(clubId, request.getPitchId());

    if (pitch != null) {
      checkPitchConflict(
          pitch.getId(),
          request.getDate(),
          request.getStartTime(),
          request.getEndTime(),
          training.getId());
    }

    training.setDate(request.getDate());
    training.setStartTime(request.getStartTime());
    training.setEndTime(request.getEndTime());
    training.setNotes(request.getNotes());
    training.setPitch(pitch);

    if (request.getStatus() != null) {
      training.setStatus(request.getStatus());
    }

    training = trainingSessionRepository.save(training);
    return trainingSessionMapper.toDto(training);
  }

  @Transactional
  public void cancelTraining(UUID clubId, UUID trainingId) {
    TrainingSession training =
        trainingSessionRepository
            .findByIdAndTeamClubId(trainingId, clubId)
            .orElseThrow(() -> new ResourceNotFoundException("TrainingSession", "id", trainingId));

    if (training.getStatus() == TrainingSessionStatus.CANCELLED) {
      throw new BadRequestException(
          msg("error.training.alreadyCancelled"), "trainingSession", "alreadyCancelled");
    }

    training.setStatus(TrainingSessionStatus.CANCELLED);
    trainingSessionRepository.save(training);
  }

  @Transactional
  public void deleteTraining(UUID clubId, UUID trainingId) {
    TrainingSession training =
        trainingSessionRepository
            .findByIdAndTeamClubId(trainingId, clubId)
            .orElseThrow(() -> new ResourceNotFoundException("TrainingSession", "id", trainingId));
    trainingSessionRepository.delete(training);
  }

  private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
    if (!endTime.isAfter(startTime)) {
      throw new BadRequestException(
          msg("error.training.endBeforeStart"), "trainingSession", "endBeforeStart");
    }
  }

  private Pitch resolvePitch(UUID clubId, UUID pitchId) {
    if (pitchId == null) {
      return null;
    }
    return pitchRepository
        .findByIdAndClubId(pitchId, clubId)
        .orElseThrow(() -> new ResourceNotFoundException("Pitch", "id", pitchId));
  }

  private void checkPitchConflict(
      UUID pitchId, LocalDate date, LocalTime start, LocalTime end, UUID excludeId) {
    List<TrainingSession> conflicts =
        excludeId != null
            ? trainingSessionRepository.findConflictingBookingsExcluding(
                pitchId, date, start, end, excludeId)
            : trainingSessionRepository.findConflictingBookings(pitchId, date, start, end);
    if (!conflicts.isEmpty()) {
      throw new ConflictException(
          msg("error.training.pitchConflict"), "trainingSession", "pitchConflict");
    }
  }

  private String msg(String key, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(key, args, key, locale);
  }
}
