package ee.finalthesis.clubmanagement.api.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.finalthesis.clubmanagement.IntegrationTest;
import ee.finalthesis.clubmanagement.domain.Club;
import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.repository.AttendanceRepository;
import ee.finalthesis.clubmanagement.repository.ClubRepository;
import ee.finalthesis.clubmanagement.repository.TeamMemberRepository;
import ee.finalthesis.clubmanagement.repository.TrainingSessionRepository;
import ee.finalthesis.clubmanagement.repository.UserRepository;
import ee.finalthesis.clubmanagement.security.JwtTokenProvider;
import ee.finalthesis.clubmanagement.security.UserPrincipal;
import ee.finalthesis.clubmanagement.service.dto.club.UpdateClubDTO;
import java.time.LocalDate;
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
class ClubControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper om;
  @Autowired private ClubRepository clubRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private AttendanceRepository attendanceRepository;
  @Autowired private TrainingSessionRepository trainingSessionRepository;
  @Autowired private TeamMemberRepository teamMemberRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtTokenProvider jwtTokenProvider;

  private Club club;
  private Club otherClub;
  private User adminUser;
  private User coachUser;
  private String adminToken;
  private String coachToken;

  @BeforeEach
  void setUp() {
    attendanceRepository.deleteAll();
    trainingSessionRepository.deleteAll();
    teamMemberRepository.deleteAll();
    userRepository.deleteAll();
    clubRepository.deleteAll();

    club =
        clubRepository.saveAndFlush(
            Club.builder()
                .name("FC Tallinn")
                .registrationCode("12345678")
                .address("Tallinn, Estonia")
                .contactEmail("info@fctallinn.ee")
                .contactPhone("+372 5551234")
                .build());

    otherClub =
        clubRepository.saveAndFlush(
            Club.builder().name("FC Tartu").registrationCode("87654321").build());

    adminUser = createUser("admin@test.com", ClubRole.ADMIN, club);
    coachUser = createUser("coach@test.com", ClubRole.COACH, club);

    adminToken = generateToken(adminUser);
    coachToken = generateToken(coachUser);
  }

  @AfterEach
  void tearDown() {
    attendanceRepository.deleteAll();
    trainingSessionRepository.deleteAll();
    teamMemberRepository.deleteAll();
    userRepository.deleteAll();
    clubRepository.deleteAll();
  }

  @Test
  void getClub_asAdmin_shouldReturnClub() throws Exception {
    mockMvc
        .perform(
            get("/api/clubs/{clubId}", club.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(club.getId().toString()))
        .andExpect(jsonPath("$.name").value("FC Tallinn"))
        .andExpect(jsonPath("$.registrationCode").value("12345678"))
        .andExpect(jsonPath("$.address").value("Tallinn, Estonia"))
        .andExpect(jsonPath("$.contactEmail").value("info@fctallinn.ee"));
  }

  @Test
  void getClub_asCoach_shouldReturnClub() throws Exception {
    mockMvc
        .perform(
            get("/api/clubs/{clubId}", club.getId())
                .header("Authorization", "Bearer " + coachToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("FC Tallinn"));
  }

  @Test
  void getClub_unauthenticated_shouldReturn401() throws Exception {
    mockMvc.perform(get("/api/clubs/{clubId}", club.getId())).andExpect(status().isUnauthorized());
  }

  @Test
  void getClub_fromWrongClub_shouldReturn403() throws Exception {
    mockMvc
        .perform(
            get("/api/clubs/{clubId}", otherClub.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void updateClub_asAdmin_shouldUpdateClub() throws Exception {
    UpdateClubDTO request = new UpdateClubDTO();
    request.setName("FC Tallinn Updated");
    request.setRegistrationCode("99999999");
    request.setAddress("New Address");
    request.setContactEmail("new@fctallinn.ee");
    request.setContactPhone("+372 5559999");

    mockMvc
        .perform(
            put("/api/clubs/{clubId}", club.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("FC Tallinn Updated"))
        .andExpect(jsonPath("$.registrationCode").value("99999999"))
        .andExpect(jsonPath("$.address").value("New Address"))
        .andExpect(jsonPath("$.contactEmail").value("new@fctallinn.ee"))
        .andExpect(jsonPath("$.contactPhone").value("+372 5559999"));
  }

  @Test
  void updateClub_asCoach_shouldReturn403() throws Exception {
    UpdateClubDTO request = new UpdateClubDTO();
    request.setName("FC Tallinn Updated");

    mockMvc
        .perform(
            put("/api/clubs/{clubId}", club.getId())
                .header("Authorization", "Bearer " + coachToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void updateClub_withInvalidData_shouldReturn400() throws Exception {
    UpdateClubDTO request = new UpdateClubDTO();
    request.setName(""); // blank name

    mockMvc
        .perform(
            put("/api/clubs/{clubId}", club.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void getClub_nonexistentClub_shouldReturn403() throws Exception {
    // Security checker denies because user's club doesn't match
    mockMvc
        .perform(
            get("/api/clubs/{clubId}", UUID.randomUUID())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isForbidden());
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
