package ee.finalthesis.clubmanagement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ee.finalthesis.clubmanagement.common.exception.BadRequestException;
import ee.finalthesis.clubmanagement.common.exception.ConflictException;
import ee.finalthesis.clubmanagement.domain.*;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.domain.enumeration.TrainingSessionStatus;
import ee.finalthesis.clubmanagement.repository.PitchRepository;
import ee.finalthesis.clubmanagement.repository.TeamMemberRepository;
import ee.finalthesis.clubmanagement.repository.TeamRepository;
import ee.finalthesis.clubmanagement.repository.TrainingSessionRepository;
import ee.finalthesis.clubmanagement.security.UserPrincipal;
import ee.finalthesis.clubmanagement.service.dto.training.CreateRecurringTrainingDTO;
import ee.finalthesis.clubmanagement.service.dto.training.CreateTrainingSessionDTO;
import ee.finalthesis.clubmanagement.service.dto.training.TrainingSessionDTO;
import ee.finalthesis.clubmanagement.service.mapper.TrainingSessionMapper;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class TrainingSessionServiceTest {

  @Mock private TrainingSessionRepository trainingSessionRepository;
  @Mock private TeamRepository teamRepository;
  @Mock private PitchRepository pitchRepository;
  @Mock private TeamMemberRepository teamMemberRepository;
  @Mock private TrainingSessionMapper trainingSessionMapper;
  @Mock private AttendanceService attendanceService;
  @Mock private MessageSource messageSource;

  @InjectMocks private TrainingSessionService trainingSessionService;

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void createTraining_withPitchConflict_shouldThrowConflict() {
    UUID clubId = UUID.randomUUID();
    UUID teamId = UUID.randomUUID();
    UUID pitchId = UUID.randomUUID();

    Team team = Team.builder().id(teamId).build();
    Pitch pitch = Pitch.builder().id(pitchId).build();

    CreateTrainingSessionDTO request = new CreateTrainingSessionDTO();
    request.setDate(LocalDate.of(2026, 4, 1));
    request.setStartTime(LocalTime.of(10, 0));
    request.setEndTime(LocalTime.of(11, 0));
    request.setPitchId(pitchId);

    when(teamRepository.findByIdAndClubId(teamId, clubId)).thenReturn(Optional.of(team));
    when(pitchRepository.findByIdAndClubId(pitchId, clubId)).thenReturn(Optional.of(pitch));
    when(trainingSessionRepository.findConflictingBookings(
            pitchId, request.getDate(), request.getStartTime(), request.getEndTime()))
        .thenReturn(List.of(new TrainingSession()));
    when(messageSource.getMessage(any(), any(), any(), any())).thenReturn("Pitch conflict");

    assertThatThrownBy(() -> trainingSessionService.createTraining(clubId, teamId, request))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void createTraining_noPitch_shouldSkipConflictCheck() {
    UUID clubId = UUID.randomUUID();
    UUID teamId = UUID.randomUUID();

    Team team = Team.builder().id(teamId).build();

    CreateTrainingSessionDTO request = new CreateTrainingSessionDTO();
    request.setDate(LocalDate.of(2026, 4, 1));
    request.setStartTime(LocalTime.of(10, 0));
    request.setEndTime(LocalTime.of(11, 0));
    request.setPitchId(null);

    TrainingSession saved = TrainingSession.builder().id(UUID.randomUUID()).team(team).build();
    TrainingSessionDTO dto = new TrainingSessionDTO();

    when(teamRepository.findByIdAndClubId(teamId, clubId)).thenReturn(Optional.of(team));
    when(trainingSessionRepository.save(any())).thenReturn(saved);
    when(trainingSessionMapper.toDto(any(TrainingSession.class))).thenReturn(dto);

    trainingSessionService.createTraining(clubId, teamId, request);

    verify(trainingSessionRepository, never()).findConflictingBookings(any(), any(), any(), any());
  }

  @Test
  void createTraining_endBeforeStart_shouldThrowBadRequest() {
    UUID clubId = UUID.randomUUID();
    UUID teamId = UUID.randomUUID();

    Team team = Team.builder().id(teamId).build();

    CreateTrainingSessionDTO request = new CreateTrainingSessionDTO();
    request.setDate(LocalDate.of(2026, 4, 1));
    request.setStartTime(LocalTime.of(11, 0));
    request.setEndTime(LocalTime.of(10, 0));

    when(teamRepository.findByIdAndClubId(teamId, clubId)).thenReturn(Optional.of(team));
    when(messageSource.getMessage(any(), any(), any(), any())).thenReturn("End before start");

    assertThatThrownBy(() -> trainingSessionService.createTraining(clubId, teamId, request))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void createRecurringTraining_shouldGenerateWeeklySessions() {
    UUID clubId = UUID.randomUUID();
    UUID teamId = UUID.randomUUID();

    Team team = Team.builder().id(teamId).build();

    CreateRecurringTrainingDTO request = new CreateRecurringTrainingDTO();
    // 2026-04-06 is a Monday
    request.setStartDate(LocalDate.of(2026, 4, 6));
    request.setEndDate(LocalDate.of(2026, 4, 27));
    request.setDayOfWeek(DayOfWeek.MONDAY);
    request.setStartTime(LocalTime.of(10, 0));
    request.setEndTime(LocalTime.of(11, 0));

    when(teamRepository.findByIdAndClubId(teamId, clubId)).thenReturn(Optional.of(team));
    when(trainingSessionRepository.save(any()))
        .thenAnswer(
            inv -> {
              TrainingSession ts = inv.getArgument(0);
              ts.setId(UUID.randomUUID());
              return ts;
            });
    when(trainingSessionMapper.toDto(anyList())).thenReturn(List.of());

    trainingSessionService.createRecurringTraining(clubId, teamId, request);

    // 4 Mondays: Apr 6, 13, 20, 27
    verify(trainingSessionRepository, times(4)).save(any(TrainingSession.class));
    verify(attendanceService, times(4)).createAttendanceForTraining(any());
  }

  @Test
  void createRecurringTraining_endDateBeforeStart_shouldThrow() {
    UUID clubId = UUID.randomUUID();
    UUID teamId = UUID.randomUUID();

    Team team = Team.builder().id(teamId).build();

    CreateRecurringTrainingDTO request = new CreateRecurringTrainingDTO();
    request.setStartDate(LocalDate.of(2026, 4, 20));
    request.setEndDate(LocalDate.of(2026, 4, 6));
    request.setDayOfWeek(DayOfWeek.MONDAY);
    request.setStartTime(LocalTime.of(10, 0));
    request.setEndTime(LocalTime.of(11, 0));

    when(teamRepository.findByIdAndClubId(teamId, clubId)).thenReturn(Optional.of(team));
    when(messageSource.getMessage(any(), any(), any(), any())).thenReturn("End date before start");

    assertThatThrownBy(
            () -> trainingSessionService.createRecurringTraining(clubId, teamId, request))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void createRecurringTraining_startDateNotOnDayOfWeek_shouldAdvance() {
    UUID clubId = UUID.randomUUID();
    UUID teamId = UUID.randomUUID();

    Team team = Team.builder().id(teamId).build();

    CreateRecurringTrainingDTO request = new CreateRecurringTrainingDTO();
    // 2026-04-06 is Monday, request Wednesday -> first session on Apr 8
    request.setStartDate(LocalDate.of(2026, 4, 6));
    request.setEndDate(LocalDate.of(2026, 4, 15));
    request.setDayOfWeek(DayOfWeek.WEDNESDAY);
    request.setStartTime(LocalTime.of(10, 0));
    request.setEndTime(LocalTime.of(11, 0));

    when(teamRepository.findByIdAndClubId(teamId, clubId)).thenReturn(Optional.of(team));
    when(trainingSessionRepository.save(any()))
        .thenAnswer(
            inv -> {
              TrainingSession ts = inv.getArgument(0);
              ts.setId(UUID.randomUUID());
              return ts;
            });
    when(trainingSessionMapper.toDto(anyList())).thenReturn(List.of());

    trainingSessionService.createRecurringTraining(clubId, teamId, request);

    // 2 Wednesdays: Apr 8, Apr 15
    ArgumentCaptor<TrainingSession> captor = ArgumentCaptor.forClass(TrainingSession.class);
    verify(trainingSessionRepository, times(2)).save(captor.capture());

    List<TrainingSession> saved = captor.getAllValues();
    assertThat(saved.get(0).getDate()).isEqualTo(LocalDate.of(2026, 4, 8));
    assertThat(saved.get(1).getDate()).isEqualTo(LocalDate.of(2026, 4, 15));
  }

  @Test
  void cancelTraining_shouldSetCancelled() {
    UUID clubId = UUID.randomUUID();
    UUID trainingId = UUID.randomUUID();

    TrainingSession training =
        TrainingSession.builder().id(trainingId).status(TrainingSessionStatus.SCHEDULED).build();

    when(trainingSessionRepository.findByIdAndTeamClubId(trainingId, clubId))
        .thenReturn(Optional.of(training));

    trainingSessionService.cancelTraining(clubId, trainingId);

    assertThat(training.getStatus()).isEqualTo(TrainingSessionStatus.CANCELLED);
    verify(trainingSessionRepository).save(training);
  }

  @Test
  void cancelTraining_alreadyCancelled_shouldThrow() {
    UUID clubId = UUID.randomUUID();
    UUID trainingId = UUID.randomUUID();

    TrainingSession training =
        TrainingSession.builder().id(trainingId).status(TrainingSessionStatus.CANCELLED).build();

    when(trainingSessionRepository.findByIdAndTeamClubId(trainingId, clubId))
        .thenReturn(Optional.of(training));
    when(messageSource.getMessage(any(), any(), any(), any())).thenReturn("Already cancelled");

    assertThatThrownBy(() -> trainingSessionService.cancelTraining(clubId, trainingId))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void listTrainings_asAdmin_shouldReturnAll() {
    UUID clubId = UUID.randomUUID();
    setSecurityContext(ClubRole.CLUB_ADMIN);

    when(trainingSessionRepository.findByTeamClubId(clubId)).thenReturn(List.of());
    when(trainingSessionMapper.toDto(anyList())).thenReturn(List.of());

    trainingSessionService.listTrainingsByClub(clubId, false);

    verify(trainingSessionRepository).findByTeamClubId(clubId);
    verify(teamMemberRepository, never()).findByUserId(any());
  }

  @Test
  void listTrainings_asNonAdmin_shouldFilterByTeamMembership() {
    UUID clubId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID teamId = UUID.randomUUID();

    setSecurityContext(userId, ClubRole.PLAYER, clubId);

    Club club = Club.builder().id(clubId).build();
    Team team = Team.builder().id(teamId).club(club).build();
    TeamMember tm = TeamMember.builder().team(team).build();

    when(teamMemberRepository.findByUserId(userId)).thenReturn(List.of(tm));
    when(trainingSessionRepository.findByTeamIdIn(List.of(teamId))).thenReturn(List.of());
    when(trainingSessionMapper.toDto(anyList())).thenReturn(List.of());

    trainingSessionService.listTrainingsByClub(clubId, false);

    verify(trainingSessionRepository).findByTeamIdIn(List.of(teamId));
    verify(trainingSessionRepository, never()).findByTeamClubId(any());
  }

  private void setSecurityContext(ClubRole role) {
    setSecurityContext(UUID.randomUUID(), role, UUID.randomUUID());
  }

  private void setSecurityContext(UUID userId, ClubRole role, UUID clubId) {
    UserPrincipal principal = UserPrincipal.fromToken(userId, "test@test.com", role, null, clubId);
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    SecurityContextHolder.getContext().setAuthentication(auth);
  }
}
