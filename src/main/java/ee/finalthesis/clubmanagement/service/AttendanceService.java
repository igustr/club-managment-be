package ee.finalthesis.clubmanagement.service;

import ee.finalthesis.clubmanagement.common.exception.ResourceNotFoundException;
import ee.finalthesis.clubmanagement.domain.Attendance;
import ee.finalthesis.clubmanagement.domain.TrainingSession;
import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.domain.enumeration.AttendanceStatus;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.domain.enumeration.SystemRole;
import ee.finalthesis.clubmanagement.repository.AttendanceRepository;
import ee.finalthesis.clubmanagement.repository.TeamMemberRepository;
import ee.finalthesis.clubmanagement.repository.TrainingSessionRepository;
import ee.finalthesis.clubmanagement.repository.UserRepository;
import ee.finalthesis.clubmanagement.security.SecurityUtils;
import ee.finalthesis.clubmanagement.service.dto.attendance.AttendanceDTO;
import ee.finalthesis.clubmanagement.service.dto.attendance.AttendanceSummaryDTO;
import ee.finalthesis.clubmanagement.service.dto.attendance.UpdateAttendanceDTO;
import ee.finalthesis.clubmanagement.service.mapper.AttendanceMapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AttendanceService {

  private final AttendanceRepository attendanceRepository;
  private final TrainingSessionRepository trainingSessionRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final UserRepository userRepository;
  private final AttendanceMapper attendanceMapper;
  private final MessageSource messageSource;

  @Transactional
  public void createAttendanceForTraining(TrainingSession training) {
    List<UUID> existingUserIds =
        attendanceRepository.findUserIdsByTrainingSessionId(training.getId());
    var existingSet = new java.util.HashSet<>(existingUserIds);

    List<Attendance> newAttendances =
        teamMemberRepository.findByTeamId(training.getTeam().getId()).stream()
            .filter(tm -> tm.getUser().getRole() == ClubRole.PLAYER)
            .filter(tm -> !existingSet.contains(tm.getUser().getId()))
            .map(tm -> Attendance.builder().trainingSession(training).user(tm.getUser()).build())
            .toList();

    if (!newAttendances.isEmpty()) {
      attendanceRepository.saveAll(newAttendances);
    }
  }

  @Transactional
  public void createAttendanceForNewTeamMember(UUID teamId, User user) {
    if (user.getRole() != ClubRole.PLAYER) {
      return;
    }
    List<TrainingSession> futureTrainings =
        trainingSessionRepository.findFutureScheduledByTeamId(teamId, LocalDate.now());

    List<Attendance> newAttendances =
        futureTrainings.stream()
            .filter(
                ts ->
                    !attendanceRepository.existsByTrainingSessionIdAndUserId(
                        ts.getId(), user.getId()))
            .map(ts -> Attendance.builder().trainingSession(ts).user(user).build())
            .toList();

    if (!newAttendances.isEmpty()) {
      attendanceRepository.saveAll(newAttendances);
    }
  }

  @Transactional(readOnly = true)
  public List<AttendanceDTO> getMyAttendances(UUID clubId) {
    UUID currentUserId =
        SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new AccessDeniedException(msg("error.attendance.notAuthorized")));
    List<Attendance> attendances = new java.util.ArrayList<>(
        attendanceRepository.findByUserIdAndTrainingSessionTeamClubId(currentUserId, clubId));

    // For parents: also include children's attendance records
    ClubRole role = SecurityUtils.getCurrentUserRole().orElse(null);
    if (role == ClubRole.PARENT) {
      List<User> children = userRepository.findChildrenByParentId(currentUserId);
      for (User child : children) {
        attendances.addAll(
            attendanceRepository.findByUserIdAndTrainingSessionTeamClubId(child.getId(), clubId));
      }
    }

    return attendanceMapper.toDto(attendances);
  }

  @Transactional
  public List<AttendanceDTO> getAttendanceList(UUID clubId, UUID trainingId) {
    TrainingSession training = findTrainingInClub(clubId, trainingId);
    createAttendanceForTraining(training);
    List<Attendance> attendances =
        attendanceRepository.findByTrainingSessionId(training.getId()).stream()
            .filter(a -> a.getUser().getRole() == ClubRole.PLAYER)
            .toList();
    return attendanceMapper.toDto(attendances);
  }

  @Transactional(readOnly = true)
  public java.util.Optional<AttendanceDTO> getMyAttendance(
      UUID clubId, UUID trainingId, UUID targetUserId) {
    findTrainingInClub(clubId, trainingId);
    UUID currentUserId =
        SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new AccessDeniedException(msg("error.attendance.notAuthorized")));

    UUID lookupUserId = targetUserId != null ? targetUserId : currentUserId;

    // If looking up another user's attendance, verify parent relationship
    if (targetUserId != null && !targetUserId.equals(currentUserId)) {
      User targetUser =
          userRepository
              .findById(targetUserId)
              .orElseThrow(() -> new ResourceNotFoundException("User", "id", targetUserId));
      boolean isParent =
          targetUser.getParents().stream().anyMatch(p -> p.getId().equals(currentUserId));
      if (!isParent) {
        throw new AccessDeniedException(msg("error.attendance.notAuthorized"));
      }
    }

    return attendanceRepository
        .findByTrainingSessionIdAndUserId(trainingId, lookupUserId)
        .map(attendanceMapper::toDto);
  }

  @Transactional
  public AttendanceSummaryDTO getAttendanceSummary(UUID clubId, UUID trainingId) {
    TrainingSession training = findTrainingInClub(clubId, trainingId);
    createAttendanceForTraining(training);
    List<Attendance> attendances =
        attendanceRepository.findByTrainingSessionId(training.getId()).stream()
            .filter(a -> a.getUser().getRole() == ClubRole.PLAYER)
            .toList();
    List<AttendanceDTO> dtos = attendanceMapper.toDto(attendances);

    AttendanceSummaryDTO summary = new AttendanceSummaryDTO();
    summary.setTotal(dtos.size());
    summary.setConfirmed(
        (int)
            attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.CONFIRMED).count());
    summary.setDeclined(
        (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.DECLINED).count());
    summary.setPending(
        (int) attendances.stream().filter(a -> a.getStatus() == AttendanceStatus.PENDING).count());
    summary.setAttendances(dtos);
    return summary;
  }

  @Transactional
  public AttendanceDTO updateAttendance(
      UUID clubId, UUID trainingId, UUID userId, UpdateAttendanceDTO request) {
    TrainingSession training = findTrainingInClub(clubId, trainingId);

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

    Attendance attendance =
        attendanceRepository
            .findByTrainingSessionIdAndUserId(trainingId, userId)
            .orElseGet(
                () -> attendanceRepository.save(
                    Attendance.builder().trainingSession(training).user(user).build()));

    UUID currentUserId =
        SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new AccessDeniedException(msg("error.attendance.notAuthorized")));

    // Allow if the current user is updating their own attendance
    if (!currentUserId.equals(userId)) {
      // Allow CLUB_ADMIN, COACH, or MASTER_ADMIN to update anyone's attendance
      ClubRole clubRole = SecurityUtils.getCurrentUserRole().orElse(null);
      SystemRole systemRole = SecurityUtils.getCurrentUserSystemRole().orElse(null);
      boolean isAdminOrCoach =
          clubRole == ClubRole.CLUB_ADMIN
              || clubRole == ClubRole.COACH
              || systemRole == SystemRole.MASTER_ADMIN;

      if (!isAdminOrCoach) {
        // Check if current user is a parent of the target user
        boolean isParent =
            user.getParents().stream().anyMatch(p -> p.getId().equals(currentUserId));
        if (!isParent) {
          throw new AccessDeniedException(msg("error.attendance.notAuthorized"));
        }
      }
    }

    attendance.setStatus(request.getStatus());
    attendance = attendanceRepository.save(attendance);
    return attendanceMapper.toDto(attendance);
  }

  private TrainingSession findTrainingInClub(UUID clubId, UUID trainingId) {
    return trainingSessionRepository
        .findByIdAndTeamClubId(trainingId, clubId)
        .orElseThrow(() -> new ResourceNotFoundException("TrainingSession", "id", trainingId));
  }

  private String msg(String key, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(key, args, key, locale);
  }
}
