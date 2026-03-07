package ee.finalthesis.clubmanagement.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.finalthesis.clubmanagement.IntegrationTest;
import ee.finalthesis.clubmanagement.domain.Attendance;
import ee.finalthesis.clubmanagement.domain.Club;
import ee.finalthesis.clubmanagement.domain.Team;
import ee.finalthesis.clubmanagement.domain.TeamMember;
import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.domain.enumeration.AttendanceStatus;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.repository.AttendanceRepository;
import ee.finalthesis.clubmanagement.repository.ClubRepository;
import ee.finalthesis.clubmanagement.repository.TeamMemberRepository;
import ee.finalthesis.clubmanagement.repository.TeamRepository;
import ee.finalthesis.clubmanagement.repository.TrainingSessionRepository;
import ee.finalthesis.clubmanagement.repository.UserRepository;
import ee.finalthesis.clubmanagement.security.JwtTokenProvider;
import ee.finalthesis.clubmanagement.security.UserPrincipal;
import ee.finalthesis.clubmanagement.service.dto.attendance.UpdateAttendanceDTO;
import ee.finalthesis.clubmanagement.service.dto.training.CreateRecurringTrainingDTO;
import ee.finalthesis.clubmanagement.service.dto.training.CreateTrainingSessionDTO;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
class AttendanceControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper om;
  @Autowired private ClubRepository clubRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private TeamRepository teamRepository;
  @Autowired private TeamMemberRepository teamMemberRepository;
  @Autowired private TrainingSessionRepository trainingSessionRepository;
  @Autowired private AttendanceRepository attendanceRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtTokenProvider jwtTokenProvider;

  private Club club;
  private User adminUser;
  private User coachUser;
  private User playerUser;
  private User parentUser;
  private User otherPlayerUser;
  private Team team;
  private String adminToken;
  private String coachToken;
  private String playerToken;
  private String parentToken;
  private String otherPlayerToken;

  @BeforeEach
  void setUp() {
    attendanceRepository.deleteAll();
    trainingSessionRepository.deleteAll();
    teamMemberRepository.deleteAll();
    teamRepository.deleteAll();
    userRepository.deleteAll();
    clubRepository.deleteAll();

    club =
        clubRepository.saveAndFlush(
            Club.builder().name("FC Tallinn").registrationCode("12345678").build());

    adminUser = createUser("admin@test.com", ClubRole.ADMIN, club);
    coachUser = createUser("coach@test.com", ClubRole.COACH, club);
    playerUser = createUser("player@test.com", ClubRole.PLAYER, club);
    otherPlayerUser = createUser("other-player@test.com", ClubRole.PLAYER, club);
    parentUser = createUser("parent@test.com", ClubRole.PARENT, club);

    // Set up parent-child relationship: parentUser is parent of playerUser
    playerUser.setParents(new HashSet<>(Set.of(parentUser)));
    playerUser = userRepository.saveAndFlush(playerUser);

    team =
        teamRepository.saveAndFlush(
            Team.builder().name("U-19").ageGroup("U19").season("2026").club(club).build());

    // Coach, player, and otherPlayer are team members
    teamMemberRepository.saveAndFlush(
        TeamMember.builder().team(team).user(coachUser).joinedDate(LocalDate.now()).build());
    teamMemberRepository.saveAndFlush(
        TeamMember.builder().team(team).user(playerUser).joinedDate(LocalDate.now()).build());
    teamMemberRepository.saveAndFlush(
        TeamMember.builder().team(team).user(otherPlayerUser).joinedDate(LocalDate.now()).build());

    adminToken = generateToken(adminUser);
    coachToken = generateToken(coachUser);
    playerToken = generateToken(playerUser);
    parentToken = generateToken(parentUser);
    otherPlayerToken = generateToken(otherPlayerUser);
  }

  @AfterEach
  void tearDown() {
    attendanceRepository.deleteAll();
    trainingSessionRepository.deleteAll();
    teamMemberRepository.deleteAll();
    teamRepository.deleteAll();
    userRepository.deleteAll();
    clubRepository.deleteAll();
  }

  // ========================
  // Auto-creation of attendance on training creation
  // ========================

  @Test
  void createTraining_shouldAutoCreateAttendanceForTeamMembers() throws Exception {
    CreateTrainingSessionDTO request = new CreateTrainingSessionDTO();
    request.setDate(LocalDate.of(2026, 7, 1));
    request.setStartTime(LocalTime.of(10, 0));
    request.setEndTime(LocalTime.of(11, 30));

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/teams/{teamId}/trainings", club.getId(), team.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isCreated());

    // 3 team members: coach, player, otherPlayer
    List<Attendance> attendances =
        attendanceRepository.findByTrainingSessionId(
            trainingSessionRepository.findByTeamId(team.getId()).get(0).getId());
    assertThat(attendances).hasSize(3);
    assertThat(attendances).allMatch(a -> a.getStatus() == AttendanceStatus.PENDING);
  }

  @Test
  void createRecurringTraining_shouldAutoCreateAttendanceForAllSessions() throws Exception {
    CreateRecurringTrainingDTO request = new CreateRecurringTrainingDTO();
    request.setStartDate(LocalDate.of(2026, 7, 6)); // Monday
    request.setEndDate(LocalDate.of(2026, 7, 20)); // 3 Mondays
    request.setDayOfWeek(DayOfWeek.MONDAY);
    request.setStartTime(LocalTime.of(10, 0));
    request.setEndTime(LocalTime.of(11, 30));

    mockMvc
        .perform(
            post(
                    "/api/clubs/{clubId}/teams/{teamId}/trainings/recurring",
                    club.getId(),
                    team.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.length()").value(3));

    // 3 sessions x 3 team members = 9 attendance records
    assertThat(attendanceRepository.findAll()).hasSize(9);
  }

  // ========================
  // GET /api/clubs/{clubId}/trainings/{trainingId}/attendance
  // ========================

  @Test
  void getAttendanceList_asCoach_shouldReturnList() throws Exception {
    UUID trainingId = createTrainingAndGetId();

    mockMvc
        .perform(
            get("/api/clubs/{clubId}/trainings/{trainingId}/attendance", club.getId(), trainingId)
                .header("Authorization", "Bearer " + coachToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].status").value("PENDING"));
  }

  @Test
  void getAttendanceList_asAdmin_shouldReturnList() throws Exception {
    UUID trainingId = createTrainingAndGetId();

    mockMvc
        .perform(
            get("/api/clubs/{clubId}/trainings/{trainingId}/attendance", club.getId(), trainingId)
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3));
  }

  @Test
  void getAttendanceList_asPlayer_shouldReturn403() throws Exception {
    UUID trainingId = createTrainingAndGetId();

    mockMvc
        .perform(
            get("/api/clubs/{clubId}/trainings/{trainingId}/attendance", club.getId(), trainingId)
                .header("Authorization", "Bearer " + playerToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void getAttendanceList_unauthenticated_shouldReturn401() throws Exception {
    UUID trainingId = createTrainingAndGetId();

    mockMvc
        .perform(
            get("/api/clubs/{clubId}/trainings/{trainingId}/attendance", club.getId(), trainingId))
        .andExpect(status().isUnauthorized());
  }

  // ========================
  // GET /api/clubs/{clubId}/trainings/{trainingId}/attendance/summary
  // ========================

  @Test
  void getAttendanceSummary_asAdmin_shouldReturnSummary() throws Exception {
    UUID trainingId = createTrainingAndGetId();

    mockMvc
        .perform(
            get(
                    "/api/clubs/{clubId}/trainings/{trainingId}/attendance/summary",
                    club.getId(),
                    trainingId)
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.total").value(3))
        .andExpect(jsonPath("$.confirmed").value(0))
        .andExpect(jsonPath("$.declined").value(0))
        .andExpect(jsonPath("$.pending").value(3))
        .andExpect(jsonPath("$.attendances.length()").value(3));
  }

  @Test
  void getAttendanceSummary_asPlayer_shouldReturn403() throws Exception {
    UUID trainingId = createTrainingAndGetId();

    mockMvc
        .perform(
            get(
                    "/api/clubs/{clubId}/trainings/{trainingId}/attendance/summary",
                    club.getId(),
                    trainingId)
                .header("Authorization", "Bearer " + playerToken))
        .andExpect(status().isForbidden());
  }

  // ========================
  // PUT /api/clubs/{clubId}/trainings/{trainingId}/attendance/{userId}
  // ========================

  @Test
  void updateAttendance_asPlayerSelf_shouldConfirm() throws Exception {
    UUID trainingId = createTrainingAndGetId();

    UpdateAttendanceDTO request = new UpdateAttendanceDTO();
    request.setStatus(AttendanceStatus.CONFIRMED);

    mockMvc
        .perform(
            put(
                    "/api/clubs/{clubId}/trainings/{trainingId}/attendance/{userId}",
                    club.getId(),
                    trainingId,
                    playerUser.getId())
                .header("Authorization", "Bearer " + playerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.userId").value(playerUser.getId().toString()));
  }

  @Test
  void updateAttendance_asParentForChild_shouldConfirm() throws Exception {
    UUID trainingId = createTrainingAndGetId();

    UpdateAttendanceDTO request = new UpdateAttendanceDTO();
    request.setStatus(AttendanceStatus.CONFIRMED);

    mockMvc
        .perform(
            put(
                    "/api/clubs/{clubId}/trainings/{trainingId}/attendance/{userId}",
                    club.getId(),
                    trainingId,
                    playerUser.getId())
                .header("Authorization", "Bearer " + parentToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"))
        .andExpect(jsonPath("$.userId").value(playerUser.getId().toString()));
  }

  @Test
  void updateAttendance_asDifferentPlayer_shouldReturn403() throws Exception {
    UUID trainingId = createTrainingAndGetId();

    UpdateAttendanceDTO request = new UpdateAttendanceDTO();
    request.setStatus(AttendanceStatus.CONFIRMED);

    mockMvc
        .perform(
            put(
                    "/api/clubs/{clubId}/trainings/{trainingId}/attendance/{userId}",
                    club.getId(),
                    trainingId,
                    playerUser.getId())
                .header("Authorization", "Bearer " + otherPlayerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void updateAttendance_asCoach_shouldReturn403() throws Exception {
    UUID trainingId = createTrainingAndGetId();

    UpdateAttendanceDTO request = new UpdateAttendanceDTO();
    request.setStatus(AttendanceStatus.CONFIRMED);

    // Coach tries to confirm player's attendance — not allowed
    mockMvc
        .perform(
            put(
                    "/api/clubs/{clubId}/trainings/{trainingId}/attendance/{userId}",
                    club.getId(),
                    trainingId,
                    playerUser.getId())
                .header("Authorization", "Bearer " + coachToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void updateAttendance_withDeclined_shouldDecline() throws Exception {
    UUID trainingId = createTrainingAndGetId();

    UpdateAttendanceDTO request = new UpdateAttendanceDTO();
    request.setStatus(AttendanceStatus.DECLINED);

    mockMvc
        .perform(
            put(
                    "/api/clubs/{clubId}/trainings/{trainingId}/attendance/{userId}",
                    club.getId(),
                    trainingId,
                    playerUser.getId())
                .header("Authorization", "Bearer " + playerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("DECLINED"));
  }

  @Test
  void updateAttendance_nonExistentUser_shouldReturn404() throws Exception {
    UUID trainingId = createTrainingAndGetId();

    UpdateAttendanceDTO request = new UpdateAttendanceDTO();
    request.setStatus(AttendanceStatus.CONFIRMED);

    mockMvc
        .perform(
            put(
                    "/api/clubs/{clubId}/trainings/{trainingId}/attendance/{userId}",
                    club.getId(),
                    trainingId,
                    UUID.randomUUID())
                .header("Authorization", "Bearer " + playerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isNotFound());
  }

  // ========================
  // Helper methods
  // ========================

  private UUID createTrainingAndGetId() throws Exception {
    CreateTrainingSessionDTO request = new CreateTrainingSessionDTO();
    request.setDate(LocalDate.of(2026, 7, 1));
    request.setStartTime(LocalTime.of(10, 0));
    request.setEndTime(LocalTime.of(11, 30));

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/teams/{teamId}/trainings", club.getId(), team.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isCreated());

    return trainingSessionRepository.findByTeamId(team.getId()).get(0).getId();
  }

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
