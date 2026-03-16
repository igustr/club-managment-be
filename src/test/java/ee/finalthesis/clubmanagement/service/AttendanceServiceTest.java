package ee.finalthesis.clubmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ee.finalthesis.clubmanagement.common.exception.ResourceNotFoundException;
import ee.finalthesis.clubmanagement.domain.*;
import ee.finalthesis.clubmanagement.domain.enumeration.AttendanceStatus;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.repository.AttendanceRepository;
import ee.finalthesis.clubmanagement.repository.TeamMemberRepository;
import ee.finalthesis.clubmanagement.repository.TrainingSessionRepository;
import ee.finalthesis.clubmanagement.repository.UserRepository;
import ee.finalthesis.clubmanagement.security.UserPrincipal;
import ee.finalthesis.clubmanagement.service.dto.attendance.AttendanceDTO;
import ee.finalthesis.clubmanagement.service.dto.attendance.AttendanceSummaryDTO;
import ee.finalthesis.clubmanagement.service.dto.attendance.UpdateAttendanceDTO;
import ee.finalthesis.clubmanagement.service.mapper.AttendanceMapper;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

  @Mock private AttendanceRepository attendanceRepository;
  @Mock private TrainingSessionRepository trainingSessionRepository;
  @Mock private TeamMemberRepository teamMemberRepository;
  @Mock private UserRepository userRepository;
  @Mock private AttendanceMapper attendanceMapper;
  @Mock private MessageSource messageSource;

  @InjectMocks private AttendanceService attendanceService;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void updateAttendance_asSelf_shouldUpdate() {
    UUID clubId = UUID.randomUUID();
    UUID trainingId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    setSecurityContext(userId);

    TrainingSession training = TrainingSession.builder().id(trainingId).build();
    Attendance attendance =
        Attendance.builder()
            .id(UUID.randomUUID())
            .trainingSession(training)
            .status(AttendanceStatus.PENDING)
            .build();

    UpdateAttendanceDTO request = new UpdateAttendanceDTO();
    request.setStatus(AttendanceStatus.CONFIRMED);

    AttendanceDTO dto = new AttendanceDTO();

    when(trainingSessionRepository.findByIdAndTeamClubId(trainingId, clubId))
        .thenReturn(Optional.of(training));
    when(attendanceRepository.findByTrainingSessionIdAndUserId(trainingId, userId))
        .thenReturn(Optional.of(attendance));
    when(attendanceRepository.save(any())).thenReturn(attendance);
    when(attendanceMapper.toDto(any(Attendance.class))).thenReturn(dto);

    attendanceService.updateAttendance(clubId, trainingId, userId, request);

    assertThat(attendance.getStatus()).isEqualTo(AttendanceStatus.CONFIRMED);
  }

  @Test
  void updateAttendance_asParent_shouldUpdate() {
    UUID clubId = UUID.randomUUID();
    UUID trainingId = UUID.randomUUID();
    UUID childId = UUID.randomUUID();
    UUID parentId = UUID.randomUUID();

    setSecurityContext(parentId);

    TrainingSession training = TrainingSession.builder().id(trainingId).build();
    Attendance attendance =
        Attendance.builder()
            .id(UUID.randomUUID())
            .trainingSession(training)
            .status(AttendanceStatus.PENDING)
            .build();

    User parent = User.builder().id(parentId).build();
    User child = User.builder().id(childId).parents(Set.of(parent)).build();

    UpdateAttendanceDTO request = new UpdateAttendanceDTO();
    request.setStatus(AttendanceStatus.CONFIRMED);

    AttendanceDTO dto = new AttendanceDTO();

    when(trainingSessionRepository.findByIdAndTeamClubId(trainingId, clubId))
        .thenReturn(Optional.of(training));
    when(attendanceRepository.findByTrainingSessionIdAndUserId(trainingId, childId))
        .thenReturn(Optional.of(attendance));
    when(userRepository.findById(childId)).thenReturn(Optional.of(child));
    when(attendanceRepository.save(any())).thenReturn(attendance);
    when(attendanceMapper.toDto(any(Attendance.class))).thenReturn(dto);

    attendanceService.updateAttendance(clubId, trainingId, childId, request);

    assertThat(attendance.getStatus()).isEqualTo(AttendanceStatus.CONFIRMED);
  }

  @Test
  void updateAttendance_asOtherUser_shouldThrowAccessDenied() {
    UUID clubId = UUID.randomUUID();
    UUID trainingId = UUID.randomUUID();
    UUID targetUserId = UUID.randomUUID();
    UUID currentUserId = UUID.randomUUID();

    setSecurityContext(currentUserId);

    TrainingSession training = TrainingSession.builder().id(trainingId).build();
    Attendance attendance = Attendance.builder().id(UUID.randomUUID()).build();
    User targetUser = User.builder().id(targetUserId).parents(Set.of()).build();

    UpdateAttendanceDTO request = new UpdateAttendanceDTO();
    request.setStatus(AttendanceStatus.CONFIRMED);

    when(trainingSessionRepository.findByIdAndTeamClubId(trainingId, clubId))
        .thenReturn(Optional.of(training));
    when(attendanceRepository.findByTrainingSessionIdAndUserId(trainingId, targetUserId))
        .thenReturn(Optional.of(attendance));
    when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
    when(messageSource.getMessage(any(), any(), any(), any())).thenReturn("Not authorized");

    assertThatThrownBy(
            () -> attendanceService.updateAttendance(clubId, trainingId, targetUserId, request))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void getAttendanceSummary_shouldCountCorrectly() {
    UUID clubId = UUID.randomUUID();
    UUID trainingId = UUID.randomUUID();

    TrainingSession training = TrainingSession.builder().id(trainingId).build();

    Attendance a1 =
        Attendance.builder().id(UUID.randomUUID()).status(AttendanceStatus.CONFIRMED).build();
    Attendance a2 =
        Attendance.builder().id(UUID.randomUUID()).status(AttendanceStatus.CONFIRMED).build();
    Attendance a3 =
        Attendance.builder().id(UUID.randomUUID()).status(AttendanceStatus.DECLINED).build();
    Attendance a4 =
        Attendance.builder().id(UUID.randomUUID()).status(AttendanceStatus.PENDING).build();

    List<Attendance> attendances = List.of(a1, a2, a3, a4);

    when(trainingSessionRepository.findByIdAndTeamClubId(trainingId, clubId))
        .thenReturn(Optional.of(training));
    when(attendanceRepository.findByTrainingSessionId(trainingId)).thenReturn(attendances);
    when(attendanceMapper.toDto(anyList()))
        .thenReturn(
            List.of(
                new AttendanceDTO(),
                new AttendanceDTO(),
                new AttendanceDTO(),
                new AttendanceDTO()));

    AttendanceSummaryDTO summary = attendanceService.getAttendanceSummary(clubId, trainingId);

    assertThat(summary.getTotal()).isEqualTo(4);
    assertThat(summary.getConfirmed()).isEqualTo(2);
    assertThat(summary.getDeclined()).isEqualTo(1);
    assertThat(summary.getPending()).isEqualTo(1);
  }

  @Test
  void createAttendanceForTraining_shouldCreateForAllMembers() {
    UUID trainingId = UUID.randomUUID();
    UUID teamId = UUID.randomUUID();

    Team team = Team.builder().id(teamId).build();
    TrainingSession training = TrainingSession.builder().id(trainingId).team(team).build();

    User user1 = User.builder().id(UUID.randomUUID()).build();
    User user2 = User.builder().id(UUID.randomUUID()).build();

    TeamMember tm1 = TeamMember.builder().user(user1).team(team).build();
    TeamMember tm2 = TeamMember.builder().user(user2).team(team).build();

    when(teamMemberRepository.findByTeamId(teamId)).thenReturn(List.of(tm1, tm2));
    when(attendanceRepository.existsByTrainingSessionIdAndUserId(any(), any())).thenReturn(false);

    attendanceService.createAttendanceForTraining(training);

    verify(attendanceRepository, times(2)).save(any(Attendance.class));
  }

  @Test
  void createAttendanceForTraining_shouldSkipExisting() {
    UUID trainingId = UUID.randomUUID();
    UUID teamId = UUID.randomUUID();

    Team team = Team.builder().id(teamId).build();
    TrainingSession training = TrainingSession.builder().id(trainingId).team(team).build();

    User user1 = User.builder().id(UUID.randomUUID()).build();
    User user2 = User.builder().id(UUID.randomUUID()).build();

    TeamMember tm1 = TeamMember.builder().user(user1).team(team).build();
    TeamMember tm2 = TeamMember.builder().user(user2).team(team).build();

    when(teamMemberRepository.findByTeamId(teamId)).thenReturn(List.of(tm1, tm2));
    when(attendanceRepository.existsByTrainingSessionIdAndUserId(trainingId, user1.getId()))
        .thenReturn(true);
    when(attendanceRepository.existsByTrainingSessionIdAndUserId(trainingId, user2.getId()))
        .thenReturn(false);

    attendanceService.createAttendanceForTraining(training);

    verify(attendanceRepository, times(1)).save(any(Attendance.class));
  }

  @Test
  void updateAttendance_notFound_shouldThrowNotFound() {
    UUID clubId = UUID.randomUUID();
    UUID trainingId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    setSecurityContext(userId);

    TrainingSession training = TrainingSession.builder().id(trainingId).build();

    UpdateAttendanceDTO request = new UpdateAttendanceDTO();
    request.setStatus(AttendanceStatus.CONFIRMED);

    when(trainingSessionRepository.findByIdAndTeamClubId(trainingId, clubId))
        .thenReturn(Optional.of(training));
    when(attendanceRepository.findByTrainingSessionIdAndUserId(trainingId, userId))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> attendanceService.updateAttendance(clubId, trainingId, userId, request))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  private void setSecurityContext(UUID userId) {
    UserPrincipal principal =
        UserPrincipal.fromToken(userId, "test@test.com", ClubRole.PLAYER, null, UUID.randomUUID());
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }
}
