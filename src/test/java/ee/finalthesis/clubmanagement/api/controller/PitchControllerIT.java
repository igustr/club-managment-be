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
import ee.finalthesis.clubmanagement.domain.TrainingSession;
import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.repository.ClubRepository;
import ee.finalthesis.clubmanagement.repository.PitchRepository;
import ee.finalthesis.clubmanagement.repository.TeamMemberRepository;
import ee.finalthesis.clubmanagement.repository.TeamRepository;
import ee.finalthesis.clubmanagement.repository.TrainingSessionRepository;
import ee.finalthesis.clubmanagement.repository.UserRepository;
import ee.finalthesis.clubmanagement.security.JwtTokenProvider;
import ee.finalthesis.clubmanagement.security.UserPrincipal;
import ee.finalthesis.clubmanagement.service.dto.pitch.CreatePitchDTO;
import ee.finalthesis.clubmanagement.service.dto.pitch.UpdatePitchDTO;
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
class PitchControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper om;
  @Autowired private ClubRepository clubRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private PitchRepository pitchRepository;
  @Autowired private TeamRepository teamRepository;
  @Autowired private TeamMemberRepository teamMemberRepository;
  @Autowired private TrainingSessionRepository trainingSessionRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtTokenProvider jwtTokenProvider;

  private Club club;
  private Club otherClub;
  private User adminUser;
  private User coachUser;
  private Pitch pitch;
  private String adminToken;
  private String coachToken;

  @BeforeEach
  void setUp() {
    trainingSessionRepository.deleteAll();
    teamMemberRepository.deleteAll();
    teamRepository.deleteAll();
    pitchRepository.deleteAll();
    userRepository.deleteAll();
    clubRepository.deleteAll();

    club =
        clubRepository.saveAndFlush(
            Club.builder().name("FC Tallinn").registrationCode("12345678").build());
    otherClub =
        clubRepository.saveAndFlush(
            Club.builder().name("FC Tartu").registrationCode("87654321").build());

    adminUser = createUser("admin@test.com", ClubRole.ADMIN, club);
    coachUser = createUser("coach@test.com", ClubRole.COACH, club);

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
  }

  @AfterEach
  void tearDown() {
    trainingSessionRepository.deleteAll();
    teamMemberRepository.deleteAll();
    teamRepository.deleteAll();
    pitchRepository.deleteAll();
    userRepository.deleteAll();
    clubRepository.deleteAll();
  }

  // ========================
  // GET /api/clubs/{clubId}/pitches
  // ========================

  @Test
  void listPitches_asMember_shouldReturnPitches() throws Exception {
    mockMvc
        .perform(
            get("/api/clubs/{clubId}/pitches", club.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Main Field"));
  }

  @Test
  void listPitches_asNonMember_shouldReturn403() throws Exception {
    User otherUser = createUser("other@test.com", ClubRole.ADMIN, otherClub);
    String otherToken = generateToken(otherUser);

    mockMvc
        .perform(
            get("/api/clubs/{clubId}/pitches", club.getId())
                .header("Authorization", "Bearer " + otherToken))
        .andExpect(status().isForbidden());
  }

  // ========================
  // POST /api/clubs/{clubId}/pitches
  // ========================

  @Test
  void createPitch_asAdmin_shouldCreatePitch() throws Exception {
    CreatePitchDTO request = new CreatePitchDTO();
    request.setName("Training Ground");
    request.setAddress("Park Ave 5");
    request.setSurfaceType("Artificial turf");
    request.setCapacity(50);

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/pitches", club.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Training Ground"))
        .andExpect(jsonPath("$.address").value("Park Ave 5"))
        .andExpect(jsonPath("$.surfaceType").value("Artificial turf"))
        .andExpect(jsonPath("$.capacity").value(50))
        .andExpect(jsonPath("$.clubId").value(club.getId().toString()));

    assertThat(pitchRepository.findByClubId(club.getId())).hasSize(2);
  }

  @Test
  void createPitch_asCoach_shouldReturn403() throws Exception {
    CreatePitchDTO request = new CreatePitchDTO();
    request.setName("Training Ground");

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/pitches", club.getId())
                .header("Authorization", "Bearer " + coachToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isForbidden());
  }

  // ========================
  // GET /api/clubs/{clubId}/pitches/{pitchId}
  // ========================

  @Test
  void getPitch_shouldReturnPitch() throws Exception {
    mockMvc
        .perform(
            get("/api/clubs/{clubId}/pitches/{pitchId}", club.getId(), pitch.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(pitch.getId().toString()))
        .andExpect(jsonPath("$.name").value("Main Field"));
  }

  // ========================
  // PUT /api/clubs/{clubId}/pitches/{pitchId}
  // ========================

  @Test
  void updatePitch_asAdmin_shouldUpdatePitch() throws Exception {
    UpdatePitchDTO request = new UpdatePitchDTO();
    request.setName("Main Field Updated");
    request.setAddress("New Address 10");
    request.setSurfaceType("Artificial turf");
    request.setCapacity(200);

    mockMvc
        .perform(
            put("/api/clubs/{clubId}/pitches/{pitchId}", club.getId(), pitch.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Main Field Updated"))
        .andExpect(jsonPath("$.address").value("New Address 10"))
        .andExpect(jsonPath("$.capacity").value(200));
  }

  @Test
  void updatePitch_withInvalidData_shouldReturn400() throws Exception {
    UpdatePitchDTO request = new UpdatePitchDTO();
    // name is blank — violates @NotBlank
    request.setName("");

    mockMvc
        .perform(
            put("/api/clubs/{clubId}/pitches/{pitchId}", club.getId(), pitch.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }

  // ========================
  // DELETE /api/clubs/{clubId}/pitches/{pitchId}
  // ========================

  @Test
  void deletePitch_asAdmin_shouldDeletePitch() throws Exception {
    mockMvc
        .perform(
            delete("/api/clubs/{clubId}/pitches/{pitchId}", club.getId(), pitch.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());

    assertThat(pitchRepository.findByIdAndClubId(pitch.getId(), club.getId())).isEmpty();
  }

  // ========================
  // GET /api/clubs/{clubId}/pitches/{pitchId}/schedule
  // ========================

  @Test
  void getPitchSchedule_shouldReturnTrainingsAtPitch() throws Exception {
    Team team =
        teamRepository.saveAndFlush(
            Team.builder().name("U-19").ageGroup("U19").season("2026").club(club).build());

    trainingSessionRepository.saveAndFlush(
        TrainingSession.builder()
            .date(LocalDate.of(2026, 6, 1))
            .startTime(LocalTime.of(10, 0))
            .endTime(LocalTime.of(11, 30))
            .team(team)
            .pitch(pitch)
            .build());

    mockMvc
        .perform(
            get("/api/clubs/{clubId}/pitches/{pitchId}/schedule", club.getId(), pitch.getId())
                .param("startDate", "2026-06-01")
                .param("endDate", "2026-06-30")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].pitchName").value("Main Field"));
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
