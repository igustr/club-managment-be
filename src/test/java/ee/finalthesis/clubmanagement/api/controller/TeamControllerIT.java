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
import ee.finalthesis.clubmanagement.domain.Team;
import ee.finalthesis.clubmanagement.domain.TeamMember;
import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.repository.AttendanceRepository;
import ee.finalthesis.clubmanagement.repository.ClubRepository;
import ee.finalthesis.clubmanagement.repository.TeamMemberRepository;
import ee.finalthesis.clubmanagement.repository.TeamRepository;
import ee.finalthesis.clubmanagement.repository.TrainingSessionRepository;
import ee.finalthesis.clubmanagement.repository.UserRepository;
import ee.finalthesis.clubmanagement.security.JwtTokenProvider;
import ee.finalthesis.clubmanagement.security.UserPrincipal;
import ee.finalthesis.clubmanagement.service.dto.team.AddTeamMemberDTO;
import ee.finalthesis.clubmanagement.service.dto.team.CreateTeamDTO;
import ee.finalthesis.clubmanagement.service.dto.team.UpdateTeamDTO;
import java.time.LocalDate;
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
class TeamControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper om;
  @Autowired private ClubRepository clubRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private TeamRepository teamRepository;
  @Autowired private TeamMemberRepository teamMemberRepository;
  @Autowired private AttendanceRepository attendanceRepository;
  @Autowired private TrainingSessionRepository trainingSessionRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtTokenProvider jwtTokenProvider;

  private Club club;
  private User adminUser;
  private User coachUser;
  private User playerUser;
  private Team teamA;
  private Team teamB;
  private String adminToken;
  private String coachToken;

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

    teamA =
        teamRepository.saveAndFlush(
            Team.builder().name("U-19").ageGroup("U19").season("2026").club(club).build());
    teamB =
        teamRepository.saveAndFlush(
            Team.builder().name("U-15").ageGroup("U15").season("2026").club(club).build());

    // Coach is member of teamA only
    teamMemberRepository.saveAndFlush(
        TeamMember.builder().team(teamA).user(coachUser).joinedDate(LocalDate.now()).build());

    adminToken = generateToken(adminUser);
    coachToken = generateToken(coachUser);
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
  // GET /api/clubs/{clubId}/teams
  // ========================

  @Test
  void listTeams_asAdmin_shouldReturnAllTeams() throws Exception {
    mockMvc
        .perform(
            get("/api/clubs/{clubId}/teams", club.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void listTeams_asCoach_shouldReturnOnlyOwnTeams() throws Exception {
    mockMvc
        .perform(
            get("/api/clubs/{clubId}/teams", club.getId())
                .header("Authorization", "Bearer " + coachToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("U-19"));
  }

  // ========================
  // POST /api/clubs/{clubId}/teams
  // ========================

  @Test
  void createTeam_asAdmin_shouldCreateTeam() throws Exception {
    CreateTeamDTO request = new CreateTeamDTO();
    request.setName("U-17");
    request.setAgeGroup("U17");
    request.setSeason("2026");

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/teams", club.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("U-17"))
        .andExpect(jsonPath("$.ageGroup").value("U17"))
        .andExpect(jsonPath("$.season").value("2026"))
        .andExpect(jsonPath("$.clubId").value(club.getId().toString()));

    assertThat(teamRepository.findByClubId(club.getId())).hasSize(3);
  }

  @Test
  void createTeam_asCoach_shouldReturn403() throws Exception {
    CreateTeamDTO request = new CreateTeamDTO();
    request.setName("U-17");

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/teams", club.getId())
                .header("Authorization", "Bearer " + coachToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isForbidden());
  }

  // ========================
  // GET /api/clubs/{clubId}/teams/{teamId}
  // ========================

  @Test
  void getTeam_asTeamMember_shouldReturnTeam() throws Exception {
    mockMvc
        .perform(
            get("/api/clubs/{clubId}/teams/{teamId}", club.getId(), teamA.getId())
                .header("Authorization", "Bearer " + coachToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(teamA.getId().toString()))
        .andExpect(jsonPath("$.name").value("U-19"));
  }

  @Test
  void getTeam_asNonMemberCoach_shouldReturn403() throws Exception {
    // Coach is not a member of teamB
    mockMvc
        .perform(
            get("/api/clubs/{clubId}/teams/{teamId}", club.getId(), teamB.getId())
                .header("Authorization", "Bearer " + coachToken))
        .andExpect(status().isForbidden());
  }

  // ========================
  // PUT /api/clubs/{clubId}/teams/{teamId}
  // ========================

  @Test
  void updateTeam_asAdmin_shouldUpdateTeam() throws Exception {
    UpdateTeamDTO request = new UpdateTeamDTO();
    request.setName("U-19 Updated");
    request.setAgeGroup("U19");
    request.setSeason("2027");

    mockMvc
        .perform(
            put("/api/clubs/{clubId}/teams/{teamId}", club.getId(), teamA.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("U-19 Updated"))
        .andExpect(jsonPath("$.season").value("2027"));
  }

  // ========================
  // DELETE /api/clubs/{clubId}/teams/{teamId}
  // ========================

  @Test
  void deleteTeam_asAdmin_shouldDeleteTeam() throws Exception {
    // Remove team members first (teamB has none)
    mockMvc
        .perform(
            delete("/api/clubs/{clubId}/teams/{teamId}", club.getId(), teamB.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());

    assertThat(teamRepository.findByIdAndClubId(teamB.getId(), club.getId())).isEmpty();
  }

  // ========================
  // GET /api/clubs/{clubId}/teams/{teamId}/members
  // ========================

  @Test
  void listTeamMembers_shouldReturnMembers() throws Exception {
    mockMvc
        .perform(
            get("/api/clubs/{clubId}/teams/{teamId}/members", club.getId(), teamA.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].email").value("coach@test.com"))
        .andExpect(jsonPath("$[0].role").value("COACH"));
  }

  // ========================
  // POST /api/clubs/{clubId}/teams/{teamId}/members
  // ========================

  @Test
  void addTeamMember_shouldAddUser() throws Exception {
    AddTeamMemberDTO request = new AddTeamMemberDTO();
    request.setUserId(playerUser.getId());

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/teams/{teamId}/members", club.getId(), teamA.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.userId").value(playerUser.getId().toString()))
        .andExpect(jsonPath("$.teamId").value(teamA.getId().toString()))
        .andExpect(jsonPath("$.firstName").value("Test"))
        .andExpect(jsonPath("$.role").value("PLAYER"));
  }

  @Test
  void addTeamMember_duplicate_shouldReturn409() throws Exception {
    // Coach is already a member of teamA
    AddTeamMemberDTO request = new AddTeamMemberDTO();
    request.setUserId(coachUser.getId());

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/teams/{teamId}/members", club.getId(), teamA.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isConflict());
  }

  @Test
  void addTeamMember_userNotInClub_shouldReturn400() throws Exception {
    User unaffiliatedUser = createUser("unaffiliated@test.com", null, null);

    AddTeamMemberDTO request = new AddTeamMemberDTO();
    request.setUserId(unaffiliatedUser.getId());

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/teams/{teamId}/members", club.getId(), teamA.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }

  // ========================
  // DELETE /api/clubs/{clubId}/teams/{teamId}/members/{userId}
  // ========================

  @Test
  void removeTeamMember_shouldRemoveMember() throws Exception {
    mockMvc
        .perform(
            delete(
                    "/api/clubs/{clubId}/teams/{teamId}/members/{userId}",
                    club.getId(),
                    teamA.getId(),
                    coachUser.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());

    assertThat(teamMemberRepository.existsByTeamIdAndUserId(teamA.getId(), coachUser.getId()))
        .isFalse();
  }

  @Test
  void removeTeamMember_notAMember_shouldReturn404() throws Exception {
    mockMvc
        .perform(
            delete(
                    "/api/clubs/{clubId}/teams/{teamId}/members/{userId}",
                    club.getId(),
                    teamA.getId(),
                    playerUser.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNotFound());
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
