package ee.finalthesis.clubmanagement.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.finalthesis.clubmanagement.IntegrationTest;
import ee.finalthesis.clubmanagement.domain.Club;
import ee.finalthesis.clubmanagement.domain.Pitch;
import ee.finalthesis.clubmanagement.domain.Team;
import ee.finalthesis.clubmanagement.domain.TeamMember;
import ee.finalthesis.clubmanagement.domain.TrainingSession;
import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.domain.enumeration.TrainingSessionStatus;
import ee.finalthesis.clubmanagement.repository.AttendanceRepository;
import ee.finalthesis.clubmanagement.repository.ClubRepository;
import ee.finalthesis.clubmanagement.repository.PitchRepository;
import ee.finalthesis.clubmanagement.repository.TeamMemberRepository;
import ee.finalthesis.clubmanagement.repository.TeamRepository;
import ee.finalthesis.clubmanagement.repository.TrainingSessionRepository;
import ee.finalthesis.clubmanagement.repository.UserRepository;
import ee.finalthesis.clubmanagement.security.JwtTokenProvider;
import ee.finalthesis.clubmanagement.security.UserPrincipal;
import ee.finalthesis.clubmanagement.service.dto.training.CreateRecurringTrainingDTO;
import ee.finalthesis.clubmanagement.service.dto.training.CreateTrainingSessionDTO;
import ee.finalthesis.clubmanagement.service.dto.training.UpdateTrainingSessionDTO;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@IntegrationTest
@AutoConfigureMockMvc
class TrainingSessionControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper om;
  @Autowired private ClubRepository clubRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private TeamRepository teamRepository;
  @Autowired private TeamMemberRepository teamMemberRepository;
  @Autowired private PitchRepository pitchRepository;
  @Autowired private TrainingSessionRepository trainingSessionRepository;
  @Autowired private AttendanceRepository attendanceRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtTokenProvider jwtTokenProvider;

  private Club club;
  private User adminUser;
  private User coachUser;
  private User playerUser;
  private Team teamA;
  private Team teamB;
  private Pitch pitch;
  private String adminToken;
  private String coachToken;
  private String playerToken;

  @BeforeEach
  void setUp() {
    attendanceRepository.deleteAll();
    trainingSessionRepository.deleteAll();
    teamMemberRepository.deleteAll();
    teamRepository.deleteAll();
    pitchRepository.deleteAll();
    userRepository.deleteAll();
    clubRepository.deleteAll();

    club =
        clubRepository.saveAndFlush(
            Club.builder().name("FC Tallinn").registrationCode("12345678").build());

    adminUser = createUser("admin@test.com", ClubRole.ADMIN, club);
    coachUser = createUser("coach@test.com", ClubRole.COACH, club);
    playerUser = createUser("player@test.com", ClubRole.PLAYER, club);

    teamA =
        teamRepository.saveAndFlush(
            Team.builder().name("U-19").ageGroup("U19").season("2026").club(club).build());
    teamB =
        teamRepository.saveAndFlush(
            Team.builder().name("U-15").ageGroup("U15").season("2026").club(club).build());

    // Coach is member of teamA only
    teamMemberRepository.saveAndFlush(
        TeamMember.builder().team(teamA).user(coachUser).joinedDate(LocalDate.now()).build());
    // Player is member of teamA
    teamMemberRepository.saveAndFlush(
        TeamMember.builder().team(teamA).user(playerUser).joinedDate(LocalDate.now()).build());

    pitch =
        pitchRepository.saveAndFlush(
            Pitch.builder()
                .name("Main Field")
                .address("Stadium St 1")
                .surfaceType("Natural grass")
                .capacity(100)
                .club(club)
                .build());

    adminToken = generateToken(adminUser);
    coachToken = generateToken(coachUser);
    playerToken = generateToken(playerUser);
  }

  @AfterEach
  void tearDown() {
    attendanceRepository.deleteAll();
    trainingSessionRepository.deleteAll();
    teamMemberRepository.deleteAll();
    teamRepository.deleteAll();
    pitchRepository.deleteAll();
    userRepository.deleteAll();
    clubRepository.deleteAll();
  }

  // ========================
  // POST /api/clubs/{clubId}/teams/{teamId}/trainings
  // ========================

  @Test
  void createTraining_asCoach_shouldCreateTraining() throws Exception {
    CreateTrainingSessionDTO request = new CreateTrainingSessionDTO();
    request.setDate(LocalDate.of(2026, 6, 1));
    request.setStartTime(LocalTime.of(10, 0));
    request.setEndTime(LocalTime.of(11, 30));
    request.setPitchId(pitch.getId());
    request.setNotes("Regular practice");

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/teams/{teamId}/trainings", club.getId(), teamA.getId())
                .header("Authorization", "Bearer " + coachToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.date").value("2026-06-01"))
        .andExpect(jsonPath("$.startTime").value("10:00:00"))
        .andExpect(jsonPath("$.endTime").value("11:30:00"))
        .andExpect(jsonPath("$.teamId").value(teamA.getId().toString()))
        .andExpect(jsonPath("$.teamName").value("U-19"))
        .andExpect(jsonPath("$.pitchId").value(pitch.getId().toString()))
        .andExpect(jsonPath("$.pitchName").value("Main Field"))
        .andExpect(jsonPath("$.status").value("SCHEDULED"))
        .andExpect(jsonPath("$.notes").value("Regular practice"));
  }

  @Test
  void createTraining_asPlayer_shouldReturn403() throws Exception {
    CreateTrainingSessionDTO request = new CreateTrainingSessionDTO();
    request.setDate(LocalDate.of(2026, 6, 1));
    request.setStartTime(LocalTime.of(10, 0));
    request.setEndTime(LocalTime.of(11, 30));

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/teams/{teamId}/trainings", club.getId(), teamA.getId())
                .header("Authorization", "Bearer " + playerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void createTraining_withPitchConflict_shouldReturn409() throws Exception {
    // Create existing training at 10:00-11:30
    trainingSessionRepository.saveAndFlush(
        TrainingSession.builder()
            .date(LocalDate.of(2026, 6, 1))
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(11, 30))
            .team(teamA)
            .pitch(pitch)
            .build());

    // Try to book overlapping slot
    CreateTrainingSessionDTO request = new CreateTrainingSessionDTO();
    request.setDate(LocalDate.of(2026, 6, 1));
    request.setStartTime(LocalTime.of(11, 0));
    request.setEndTime(LocalTime.of(12, 30));
    request.setPitchId(pitch.getId());

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/teams/{teamId}/trainings", club.getId(), teamA.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isConflict());
  }

  @Test
  void createTraining_withoutPitch_shouldSucceed() throws Exception {
    CreateTrainingSessionDTO request = new CreateTrainingSessionDTO();
    request.setDate(LocalDate.of(2026, 6, 1));
    request.setStartTime(LocalTime.of(10, 0));
    request.setEndTime(LocalTime.of(11, 30));

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/teams/{teamId}/trainings", club.getId(), teamA.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.pitchId").doesNotExist())
        .andExpect(jsonPath("$.pitchName").doesNotExist());
  }

  // ========================
  // POST /api/clubs/{clubId}/teams/{teamId}/trainings/recurring
  // ========================

  @Test
  void createRecurringTraining_shouldCreateMultipleSessions() throws Exception {
    CreateRecurringTrainingDTO request = new CreateRecurringTrainingDTO();
    request.setStartDate(LocalDate.of(2026, 6, 1)); // Monday
    request.setEndDate(LocalDate.of(2026, 6, 30));
    request.setDayOfWeek(DayOfWeek.MONDAY);
    request.setStartTime(LocalTime.of(10, 0));
    request.setEndTime(LocalTime.of(11, 30));
    request.setPitchId(pitch.getId());

    mockMvc
        .perform(
            post(
                    "/api/clubs/{clubId}/teams/{teamId}/trainings/recurring",
                    club.getId(),
                    teamA.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.length()").value(5)); // 5 Mondays in June 2026

    assertThat(trainingSessionRepository.findByTeamId(teamA.getId())).hasSize(5);
  }

  @Test
  void createRecurringTraining_withConflict_shouldReturn409() throws Exception {
    // Create existing training on Monday June 8 at 10:00-11:30
    trainingSessionRepository.saveAndFlush(
        TrainingSession.builder()
            .date(LocalDate.of(2026, 6, 8))
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(11, 30))
            .team(teamB)
            .pitch(pitch)
            .build());

    CreateRecurringTrainingDTO request = new CreateRecurringTrainingDTO();
    request.setStartDate(LocalDate.of(2026, 6, 1));
    request.setEndDate(LocalDate.of(2026, 6, 30));
    request.setDayOfWeek(DayOfWeek.MONDAY);
    request.setStartTime(LocalTime.of(10, 0));
    request.setEndTime(LocalTime.of(11, 30));
    request.setPitchId(pitch.getId());

    mockMvc
        .perform(
            post(
                    "/api/clubs/{clubId}/teams/{teamId}/trainings/recurring",
                    club.getId(),
                    teamA.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isConflict());
  }

  // ========================
  // GET /api/clubs/{clubId}/trainings
  // ========================

  @Test
  void listTrainings_asAdmin_shouldReturnAllClubTrainings() throws Exception {
    trainingSessionRepository.saveAndFlush(
        TrainingSession.builder()
            .date(LocalDate.of(2026, 6, 1))
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(11, 30))
            .team(teamA)
            .build());
    trainingSessionRepository.saveAndFlush(
        TrainingSession.builder()
            .date(LocalDate.of(2026, 6, 2))
            .startTime(LocalTime.of(14, 0))
            .endTime(LocalTime.of(15, 30))
            .team(teamB)
            .build());

    mockMvc
        .perform(
            get("/api/clubs/{clubId}/trainings", club.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void listTrainings_asCoach_shouldReturnOnlyOwnTeamTrainings() throws Exception {
    trainingSessionRepository.saveAndFlush(
        TrainingSession.builder()
            .date(LocalDate.of(2026, 6, 1))
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(11, 30))
            .team(teamA)
            .build());
    trainingSessionRepository.saveAndFlush(
        TrainingSession.builder()
            .date(LocalDate.of(2026, 6, 2))
            .startTime(LocalTime.of(14, 0))
            .endTime(LocalTime.of(15, 30))
            .team(teamB)
            .build());

    mockMvc
        .perform(
            get("/api/clubs/{clubId}/trainings", club.getId())
                .header("Authorization", "Bearer " + coachToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].teamName").value("U-19"));
  }

  // ========================
  // GET /api/clubs/{clubId}/trainings/{trainingId}
  // ========================

  @Test
  void getTraining_shouldReturnTraining() throws Exception {
    TrainingSession training =
        trainingSessionRepository.saveAndFlush(
            TrainingSession.builder()
                .date(LocalDate.of(2026, 6, 1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 30))
                .team(teamA)
                .pitch(pitch)
                .build());

    mockMvc
        .perform(
            get("/api/clubs/{clubId}/trainings/{trainingId}", club.getId(), training.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(training.getId().toString()))
        .andExpect(jsonPath("$.teamName").value("U-19"))
        .andExpect(jsonPath("$.pitchName").value("Main Field"));
  }

  // ========================
  // PUT /api/clubs/{clubId}/trainings/{trainingId}
  // ========================

  @Test
  void updateTraining_shouldUpdateTraining() throws Exception {
    TrainingSession training =
        trainingSessionRepository.saveAndFlush(
            TrainingSession.builder()
                .date(LocalDate.of(2026, 6, 1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 30))
                .team(teamA)
                .build());

    UpdateTrainingSessionDTO request = new UpdateTrainingSessionDTO();
    request.setDate(LocalDate.of(2026, 6, 2));
    request.setStartTime(LocalTime.of(14, 0));
    request.setEndTime(LocalTime.of(15, 30));
    request.setNotes("Updated notes");

    mockMvc
        .perform(
            put("/api/clubs/{clubId}/trainings/{trainingId}", club.getId(), training.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.date").value("2026-06-02"))
        .andExpect(jsonPath("$.startTime").value("14:00:00"))
        .andExpect(jsonPath("$.notes").value("Updated notes"));
  }

  @Test
  void updateTraining_withPitchConflict_shouldReturn409() throws Exception {
    // Existing training at pitch
    trainingSessionRepository.saveAndFlush(
        TrainingSession.builder()
            .date(LocalDate.of(2026, 6, 1))
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(11, 30))
            .team(teamB)
            .pitch(pitch)
            .build());

    // Training to update (no pitch yet)
    TrainingSession training =
        trainingSessionRepository.saveAndFlush(
            TrainingSession.builder()
                .date(LocalDate.of(2026, 6, 1))
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(15, 30))
                .team(teamA)
                .build());

    // Update to overlapping time with same pitch
    UpdateTrainingSessionDTO request = new UpdateTrainingSessionDTO();
    request.setDate(LocalDate.of(2026, 6, 1));
    request.setStartTime(LocalTime.of(10, 30));
    request.setEndTime(LocalTime.of(12, 0));
    request.setPitchId(pitch.getId());

    mockMvc
        .perform(
            put("/api/clubs/{clubId}/trainings/{trainingId}", club.getId(), training.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isConflict());
  }

  // ========================
  // PUT /api/clubs/{clubId}/trainings/{trainingId}/cancel
  // ========================

  @Test
  void cancelTraining_shouldSetStatusToCancelled() throws Exception {
    TrainingSession training =
        trainingSessionRepository.saveAndFlush(
            TrainingSession.builder()
                .date(LocalDate.of(2026, 6, 1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 30))
                .team(teamA)
                .build());

    mockMvc
        .perform(
            put("/api/clubs/{clubId}/trainings/{trainingId}/cancel", club.getId(), training.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());

    TrainingSession updated = trainingSessionRepository.findById(training.getId()).orElseThrow();
    assertThat(updated.getStatus()).isEqualTo(TrainingSessionStatus.CANCELLED);
  }

  // ========================
  // DELETE /api/clubs/{clubId}/trainings/{trainingId}
  // ========================

  @Test
  void deleteTraining_asAdmin_shouldDeleteTraining() throws Exception {
    TrainingSession training =
        trainingSessionRepository.saveAndFlush(
            TrainingSession.builder()
                .date(LocalDate.of(2026, 6, 1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 30))
                .team(teamA)
                .build());

    mockMvc
        .perform(
            delete("/api/clubs/{clubId}/trainings/{trainingId}", club.getId(), training.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());

    assertThat(trainingSessionRepository.findById(training.getId())).isEmpty();
  }

  @Test
  void deleteTraining_asCoach_shouldReturn403() throws Exception {
    TrainingSession training =
        trainingSessionRepository.saveAndFlush(
            TrainingSession.builder()
                .date(LocalDate.of(2026, 6, 1))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 30))
                .team(teamA)
                .build());

    mockMvc
        .perform(
            delete("/api/clubs/{clubId}/trainings/{trainingId}", club.getId(), training.getId())
                .header("Authorization", "Bearer " + coachToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void listTrainings_unauthenticated_shouldReturn401() throws Exception {
    mockMvc
        .perform(get("/api/clubs/{clubId}/trainings", club.getId()))
        .andExpect(status().isUnauthorized());
  }

  // ========================
  // Helper methods
  // ========================

  private User createUser(String email, ClubRole role, Club userClub) {
    User user =
        User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode("password123"))
            .firstName("Test")
            .lastName("User")
            .dateOfBirth(LocalDate.of(2000, 1, 15))
            .phone("+372 5551234")
            .role(role)
            .club(userClub)
            .active(true)
            .build();
    return userRepository.saveAndFlush(user);
  }

  private String generateToken(User user) {
    UserPrincipal principal = UserPrincipal.create(user);
    return jwtTokenProvider.generateAccessToken(principal);
  }
}
