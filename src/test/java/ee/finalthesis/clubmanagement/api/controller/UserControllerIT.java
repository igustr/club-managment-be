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
import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.repository.AttendanceRepository;
import ee.finalthesis.clubmanagement.repository.ClubRepository;
import ee.finalthesis.clubmanagement.repository.ConversationParticipantRepository;
import ee.finalthesis.clubmanagement.repository.ConversationReadStatusRepository;
import ee.finalthesis.clubmanagement.repository.ConversationRepository;
import ee.finalthesis.clubmanagement.repository.MessageRepository;
import ee.finalthesis.clubmanagement.repository.TeamMemberRepository;
import ee.finalthesis.clubmanagement.repository.TrainingSessionRepository;
import ee.finalthesis.clubmanagement.repository.UserRepository;
import ee.finalthesis.clubmanagement.security.JwtTokenProvider;
import ee.finalthesis.clubmanagement.security.UserPrincipal;
import ee.finalthesis.clubmanagement.service.dto.user.AddUserToClubDTO;
import ee.finalthesis.clubmanagement.service.dto.user.UpdateUserDTO;
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
class UserControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper om;
  @Autowired private ClubRepository clubRepository;
  @Autowired private AttendanceRepository attendanceRepository;
  @Autowired private ConversationReadStatusRepository conversationReadStatusRepository;
  @Autowired private MessageRepository messageRepository;
  @Autowired private ConversationParticipantRepository conversationParticipantRepository;
  @Autowired private ConversationRepository conversationRepository;
  @Autowired private TrainingSessionRepository trainingSessionRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private TeamMemberRepository teamMemberRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtTokenProvider jwtTokenProvider;

  private Club club;
  private User adminUser;
  private User coachUser;
  private User playerUser;
  private User unaffiliatedUser;
  private String adminToken;
  private String coachToken;

  @BeforeEach
  void setUp() {
    conversationReadStatusRepository.deleteAll();
    messageRepository.deleteAll();
    conversationParticipantRepository.deleteAll();
    conversationRepository.deleteAll();
    attendanceRepository.deleteAll();
    trainingSessionRepository.deleteAll();
    teamMemberRepository.deleteAll();
    userRepository.deleteAll();
    clubRepository.deleteAll();

    club =
        clubRepository.saveAndFlush(
            Club.builder().name("FC Tallinn").registrationCode("12345678").build());

    adminUser = createUser("admin@test.com", ClubRole.ADMIN, club);
    coachUser = createUser("coach@test.com", ClubRole.COACH, club);
    playerUser = createUser("player@test.com", ClubRole.PLAYER, club);
    unaffiliatedUser = createUser("unaffiliated@test.com", null, null);

    adminToken = generateToken(adminUser);
    coachToken = generateToken(coachUser);
  }

  @AfterEach
  void tearDown() {
    conversationReadStatusRepository.deleteAll();
    messageRepository.deleteAll();
    conversationParticipantRepository.deleteAll();
    conversationRepository.deleteAll();
    attendanceRepository.deleteAll();
    trainingSessionRepository.deleteAll();
    teamMemberRepository.deleteAll();
    userRepository.deleteAll();
    clubRepository.deleteAll();
  }

  // ========================
  // GET /api/clubs/{clubId}/users
  // ========================

  @Test
  void listClubUsers_asAdmin_shouldReturnUsers() throws Exception {
    mockMvc
        .perform(
            get("/api/clubs/{clubId}/users", club.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(3))
        .andExpect(jsonPath("$.totalElements").value(3));
  }

  @Test
  void listClubUsers_asNonMember_shouldReturn403() throws Exception {
    String unaffiliatedToken = generateToken(unaffiliatedUser);

    mockMvc
        .perform(
            get("/api/clubs/{clubId}/users", club.getId())
                .header("Authorization", "Bearer " + unaffiliatedToken))
        .andExpect(status().isForbidden());
  }

  // ========================
  // POST /api/clubs/{clubId}/users
  // ========================

  @Test
  void addUserToClub_shouldAddUnaffiliatedUser() throws Exception {
    AddUserToClubDTO request = new AddUserToClubDTO();
    request.setUserId(unaffiliatedUser.getId());
    request.setRole(ClubRole.PLAYER);

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/users", club.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(unaffiliatedUser.getId().toString()))
        .andExpect(jsonPath("$.role").value("PLAYER"))
        .andExpect(jsonPath("$.clubId").value(club.getId().toString()));

    User updated = userRepository.findById(unaffiliatedUser.getId()).orElseThrow();
    assertThat(updated.getClub().getId()).isEqualTo(club.getId());
    assertThat(updated.getRole()).isEqualTo(ClubRole.PLAYER);
  }

  @Test
  void addUserToClub_userAlreadyInClub_shouldReturn409() throws Exception {
    AddUserToClubDTO request = new AddUserToClubDTO();
    request.setUserId(coachUser.getId());
    request.setRole(ClubRole.PLAYER);

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/users", club.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isConflict());
  }

  @Test
  void addUserToClub_userNotFound_shouldReturn404() throws Exception {
    AddUserToClubDTO request = new AddUserToClubDTO();
    request.setUserId(UUID.randomUUID());
    request.setRole(ClubRole.PLAYER);

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/users", club.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isNotFound());
  }

  // ========================
  // GET /api/clubs/{clubId}/users/{userId}
  // ========================

  @Test
  void getClubUser_shouldReturnUser() throws Exception {
    mockMvc
        .perform(
            get("/api/clubs/{clubId}/users/{userId}", club.getId(), coachUser.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(coachUser.getId().toString()))
        .andExpect(jsonPath("$.email").value("coach@test.com"))
        .andExpect(jsonPath("$.role").value("COACH"));
  }

  // ========================
  // PUT /api/clubs/{clubId}/users/{userId}
  // ========================

  @Test
  void updateUser_shouldUpdateRole() throws Exception {
    UpdateUserDTO request = new UpdateUserDTO();
    request.setRole(ClubRole.ADMIN);

    mockMvc
        .perform(
            put("/api/clubs/{clubId}/users/{userId}", club.getId(), coachUser.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("ADMIN"));

    User updated = userRepository.findById(coachUser.getId()).orElseThrow();
    assertThat(updated.getRole()).isEqualTo(ClubRole.ADMIN);
  }

  // ========================
  // DELETE /api/clubs/{clubId}/users/{userId}
  // ========================

  @Test
  void removeUserFromClub_shouldRemoveUser() throws Exception {
    mockMvc
        .perform(
            delete("/api/clubs/{clubId}/users/{userId}", club.getId(), playerUser.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());

    User updated = userRepository.findById(playerUser.getId()).orElseThrow();
    assertThat(updated.getClub()).isNull();
    assertThat(updated.getRole()).isNull();
  }

  @Test
  void removeUserFromClub_cannotRemoveSelf_shouldReturn400() throws Exception {
    mockMvc
        .perform(
            delete("/api/clubs/{clubId}/users/{userId}", club.getId(), adminUser.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isBadRequest());
  }

  // ========================
  // GET /api/users/unaffiliated
  // ========================

  @Test
  void listUnaffiliatedUsers_asAdmin_shouldReturnUsers() throws Exception {
    mockMvc
        .perform(get("/api/users/unaffiliated").header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].email").value("unaffiliated@test.com"));
  }

  @Test
  void listUnaffiliatedUsers_asCoach_shouldReturn403() throws Exception {
    mockMvc
        .perform(get("/api/users/unaffiliated").header("Authorization", "Bearer " + coachToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void listUnaffiliatedUsers_withSearch_shouldFilterResults() throws Exception {
    mockMvc
        .perform(
            get("/api/users/unaffiliated")
                .param("search", "unaffiliated")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1));

    mockMvc
        .perform(
            get("/api/users/unaffiliated")
                .param("search", "nonexistent")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(0));
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
