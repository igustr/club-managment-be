package ee.finalthesis.clubmanagement.service;

import ee.finalthesis.clubmanagement.common.exception.BadRequestException;
import ee.finalthesis.clubmanagement.common.exception.ResourceNotFoundException;
import ee.finalthesis.clubmanagement.domain.Pitch;
import ee.finalthesis.clubmanagement.domain.Team;
import ee.finalthesis.clubmanagement.domain.TeamMember;
import ee.finalthesis.clubmanagement.domain.TrainingSession;
import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.domain.enumeration.NotificationType;
import ee.finalthesis.clubmanagement.domain.enumeration.SystemRole;
import ee.finalthesis.clubmanagement.domain.enumeration.TrainingSessionStatus;
import ee.finalthesis.clubmanagement.repository.PitchRepository;
import ee.finalthesis.clubmanagement.repository.TeamMemberRepository;
import ee.finalthesis.clubmanagement.repository.TeamRepository;
import ee.finalthesis.clubmanagement.repository.TrainingSessionRepository;
import ee.finalthesis.clubmanagement.repository.UserRepository;
import ee.finalthesis.clubmanagement.security.SecurityUtils;
import ee.finalthesis.clubmanagement.service.dto.training.CreateRecurringTrainingDTO;
import ee.finalthesis.clubmanagement.service.dto.training.CreateTrainingSessionDTO;
import ee.finalthesis.clubmanagement.service.dto.training.TrainingSessionDTO;
import ee.finalthesis.clubmanagement.service.dto.training.UpdateTrainingSessionDTO;
import ee.finalthesis.clubmanagement.service.mapper.TrainingSessionMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
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
public class TrainingSessionService {

  private final TrainingSessionRepository trainingSessionRepository;
  private final TeamRepository teamRepository;
  private final PitchRepository pitchRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final UserRepository userRepository;
  private final TrainingSessionMapper trainingSessionMapper;
  private final AttendanceService attendanceService;
  private final NotificationService notificationService;
  private final MessageSource messageSource;

  @Transactional(readOnly = true)
  public List<TrainingSessionDTO> listTrainingsByClub(UUID clubId, boolean myTeams) {
    ClubRole role = SecurityUtils.getCurrentUserRole().orElse(null);
    UUID userId = SecurityUtils.getCurrentUserId().orElse(null);

    if (!myTeams
        && (role == ClubRole.CLUB_ADMIN
            || SecurityUtils.getCurrentUserSystemRole().orElse(null) == SystemRole.MASTER_ADMIN)) {
      return trainingSessionMapper.toDto(trainingSessionRepository.findByTeamClubId(clubId));
    }

    // Non-admin: only show trainings for teams the user belongs to
    List<UUID> teamIds =
        teamMemberRepository.findByUserId(userId).stream()
            .filter(tm -> tm.getTeam().getClub().getId().equals(clubId))
            .map(tm -> tm.getTeam().getId())
            .collect(Collectors.toList());

    // For parents: also include children's team trainings
    if (role == ClubRole.PARENT) {
      userRepository.findChildrenByParentId(userId).stream()
          .flatMap(child -> teamMemberRepository.findByUserId(child.getId()).stream())
          .filter(tm -> tm.getTeam().getClub().getId().equals(clubId))
          .map(tm -> tm.getTeam().getId())
          .forEach(id -> {
            if (!teamIds.contains(id)) teamIds.add(id);
          });
    }

    if (teamIds.isEmpty()) {
      return List.of();
    }
    return trainingSessionMapper.toDto(trainingSessionRepository.findByTeamIdIn(teamIds));
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
    BigDecimal portion = request.getPitchPortion() != null ? request.getPitchPortion() : BigDecimal.ONE;

    TrainingSession training =
        TrainingSession.builder()
            .date(request.getDate())
            .startTime(request.getStartTime())
            .endTime(request.getEndTime())
            .notes(request.getNotes())
            .team(team)
            .pitch(pitch)
            .pitchPortion(portion)
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
    BigDecimal portion = request.getPitchPortion() != null ? request.getPitchPortion() : BigDecimal.ONE;

    UUID recurrenceGroupId = UUID.randomUUID();
    List<TrainingSession> sessions = new ArrayList<>();

    LocalDate current = request.getStartDate();
    // Advance to the first matching day of week
    while (current.getDayOfWeek() != request.getDayOfWeek()) {
      current = current.plusDays(1);
    }

    while (!current.isAfter(request.getEndDate())) {
      TrainingSession training =
          TrainingSession.builder()
              .date(current)
              .startTime(request.getStartTime())
              .endTime(request.getEndTime())
              .notes(request.getNotes())
              .team(team)
              .pitch(pitch)
              .pitchPortion(portion)
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

    training.setDate(request.getDate());
    training.setStartTime(request.getStartTime());
    training.setEndTime(request.getEndTime());
    training.setNotes(request.getNotes());
    training.setPitch(pitch);
    if (request.getPitchPortion() != null) {
      training.setPitchPortion(request.getPitchPortion());
    }

    if (request.getStatus() != null) {
      training.setStatus(request.getStatus());
    }

    training = trainingSessionRepository.save(training);

    notifyTeamMembers(
        training,
        NotificationType.TRAINING_UPDATED,
        training.getTeam().getName() + " – " + training.getDate().format(DATE_FMT));

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

    notifyTeamMembers(
        training,
        NotificationType.TRAINING_CANCELLED,
        training.getTeam().getName() + " – " + training.getDate().format(DATE_FMT));
  }

  @Transactional
  public void deleteTraining(UUID clubId, UUID trainingId) {
    TrainingSession training =
        trainingSessionRepository
            .findByIdAndTeamClubId(trainingId, clubId)
            .orElseThrow(() -> new ResourceNotFoundException("TrainingSession", "id", trainingId));

    String info = training.getTeam().getName() + " – " + training.getDate().format(DATE_FMT);
    UUID teamId = training.getTeam().getId();

    // Notify before deleting so we still have the training data
    Set<User> recipients = getTeamRecipients(teamId);
    notificationService.notifyUsers(
        recipients,
        training.getTeam().getClub(),
        NotificationType.TRAINING_DELETED,
        info,
        null,
        null);

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

  /**
   * Calculate the total occupied portion of a pitch for overlapping sessions on a given date/time range.
   */
  public BigDecimal calculateOccupancy(
      UUID pitchId, LocalDate date, LocalTime start, LocalTime end, UUID excludeId) {
    List<TrainingSession> overlapping =
        excludeId != null
            ? trainingSessionRepository.findConflictingBookingsExcluding(
                pitchId, date, start, end, excludeId)
            : trainingSessionRepository.findConflictingBookings(pitchId, date, start, end);
    return overlapping.stream()
        .map(TrainingSession::getPitchPortion)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

  private void notifyTeamMembers(
      TrainingSession training, NotificationType type, String title) {
    Set<User> recipients = getTeamRecipients(training.getTeam().getId());
    notificationService.notifyUsers(
        recipients, training.getTeam().getClub(), type, title, null, training.getId());
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
