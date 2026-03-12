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
import ee.finalthesis.clubmanagement.domain.Conversation;
import ee.finalthesis.clubmanagement.domain.Team;
import ee.finalthesis.clubmanagement.domain.TeamMember;
import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.domain.enumeration.ConversationType;
import ee.finalthesis.clubmanagement.repository.AttendanceRepository;
import ee.finalthesis.clubmanagement.repository.ClubRepository;
import ee.finalthesis.clubmanagement.repository.ConversationParticipantRepository;
import ee.finalthesis.clubmanagement.repository.ConversationReadStatusRepository;
import ee.finalthesis.clubmanagement.repository.ConversationRepository;
import ee.finalthesis.clubmanagement.repository.MessageRepository;
import ee.finalthesis.clubmanagement.repository.TeamMemberRepository;
import ee.finalthesis.clubmanagement.repository.TeamRepository;
import ee.finalthesis.clubmanagement.repository.TrainingSessionRepository;
import ee.finalthesis.clubmanagement.repository.UserRepository;
import ee.finalthesis.clubmanagement.security.JwtTokenProvider;
import ee.finalthesis.clubmanagement.security.UserPrincipal;
import ee.finalthesis.clubmanagement.service.dto.user.AddUserToClubDTO;
import ee.finalthesis.clubmanagement.service.dto.user.LinkParentDTO;
import ee.finalthesis.clubmanagement.service.dto.user.UpdateUserDTO;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
  @Autowired private TeamRepository teamRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
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
    teamRepository.deleteAll();
    jdbcTemplate.execute("DELETE FROM user_parent");
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
    teamRepository.deleteAll();
    jdbcTemplate.execute("DELETE FROM user_parent");
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

  @Test
  void addUserToClub_asNonAdmin_shouldReturn403() throws Exception {
    AddUserToClubDTO request = new AddUserToClubDTO();
    request.setUserId(unaffiliatedUser.getId());
    request.setRole(ClubRole.PLAYER);

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/users", club.getId())
                .header("Authorization", "Bearer " + coachToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isForbidden());
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

  @Test
  void removeUserFromClub_asNonAdmin_shouldReturn403() throws Exception {
    mockMvc
        .perform(
            delete("/api/clubs/{clubId}/users/{userId}", club.getId(), playerUser.getId())
                .header("Authorization", "Bearer " + coachToken))
        .andExpect(status().isForbidden());
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
  // GET /api/clubs/{clubId}/users/{userId}/parents
  // ========================

  @Test
  void listParents_shouldReturnLinkedParents() throws Exception {
    User parentUser = createUser("parent@test.com", ClubRole.PARENT, club);
    playerUser.getParents().add(parentUser);
    userRepository.saveAndFlush(playerUser);

    mockMvc
        .perform(
            get("/api/clubs/{clubId}/users/{userId}/parents", club.getId(), playerUser.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(parentUser.getId().toString()));
  }

  @Test
  void listParents_asNonMember_shouldReturn403() throws Exception {
    String unaffiliatedToken = generateToken(unaffiliatedUser);

    mockMvc
        .perform(
            get("/api/clubs/{clubId}/users/{userId}/parents", club.getId(), playerUser.getId())
                .header("Authorization", "Bearer " + unaffiliatedToken))
        .andExpect(status().isForbidden());
  }

  // ========================
  // POST /api/clubs/{clubId}/users/{userId}/parents
  // ========================

  @Test
  void linkParent_asAdmin_shouldLink() throws Exception {
    User parentUser = createUser("parent@test.com", ClubRole.PARENT, club);

    LinkParentDTO request = new LinkParentDTO();
    request.setParentId(parentUser.getId());

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/users/{userId}/parents", club.getId(), playerUser.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(parentUser.getId().toString()))
        .andExpect(jsonPath("$.role").value("PARENT"));

    assertThat(userRepository.existsParentChildRelationship(playerUser.getId(), parentUser.getId()))
        .isTrue();
  }

  @Test
  void linkParent_shouldSyncTeamConversations() throws Exception {
    User parentUser = createUser("parent@test.com", ClubRole.PARENT, club);

    // Create a team with a conversation and add player to it
    Team team = teamRepository.saveAndFlush(Team.builder().name("U18").club(club).build());
    teamMemberRepository.saveAndFlush(
        TeamMember.builder().team(team).user(playerUser).joinedDate(LocalDate.now()).build());
    Conversation conversation =
        conversationRepository.saveAndFlush(
            Conversation.builder().type(ConversationType.TEAM).team(team).club(club).build());

    LinkParentDTO request = new LinkParentDTO();
    request.setParentId(parentUser.getId());

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/users/{userId}/parents", club.getId(), playerUser.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isCreated());

    // Parent should now be a participant in the team conversation
    assertThat(
            conversationParticipantRepository.existsByConversationIdAndUserId(
                conversation.getId(), parentUser.getId()))
        .isTrue();
  }

  @Test
  void linkParent_alreadyLinked_shouldReturn409() throws Exception {
    User parentUser = createUser("parent@test.com", ClubRole.PARENT, club);
    playerUser.getParents().add(parentUser);
    userRepository.saveAndFlush(playerUser);

    LinkParentDTO request = new LinkParentDTO();
    request.setParentId(parentUser.getId());

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/users/{userId}/parents", club.getId(), playerUser.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isConflict());
  }

  @Test
  void linkParent_parentRoleRequired_shouldReturn400() throws Exception {
    LinkParentDTO request = new LinkParentDTO();
    request.setParentId(coachUser.getId());

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/users/{userId}/parents", club.getId(), playerUser.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void linkParent_selfLink_shouldReturn400() throws Exception {
    LinkParentDTO request = new LinkParentDTO();
    request.setParentId(playerUser.getId());

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/users/{userId}/parents", club.getId(), playerUser.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void linkParent_parentNotInClub_shouldReturn400() throws Exception {
    User otherParent = createUser("otherparent@test.com", ClubRole.PARENT, null);

    LinkParentDTO request = new LinkParentDTO();
    request.setParentId(otherParent.getId());

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/users/{userId}/parents", club.getId(), playerUser.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void linkParent_asNonAdmin_shouldReturn403() throws Exception {
    User parentUser = createUser("parent@test.com", ClubRole.PARENT, club);

    LinkParentDTO request = new LinkParentDTO();
    request.setParentId(parentUser.getId());

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/users/{userId}/parents", club.getId(), playerUser.getId())
                .header("Authorization", "Bearer " + coachToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isForbidden());
  }

  // ========================
  // DELETE /api/clubs/{clubId}/users/{userId}/parents/{parentId}
  // ========================

  @Test
  void unlinkParent_asAdmin_shouldUnlink() throws Exception {
    User parentUser = createUser("parent@test.com", ClubRole.PARENT, club);
    playerUser.getParents().add(parentUser);
    userRepository.saveAndFlush(playerUser);

    mockMvc
        .perform(
            delete(
                    "/api/clubs/{clubId}/users/{userId}/parents/{parentId}",
                    club.getId(),
                    playerUser.getId(),
                    parentUser.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNoContent());

    assertThat(userRepository.existsParentChildRelationship(playerUser.getId(), parentUser.getId()))
        .isFalse();
  }

  @Test
  void unlinkParent_notLinked_shouldReturn404() throws Exception {
    User parentUser = createUser("parent@test.com", ClubRole.PARENT, club);

    mockMvc
        .perform(
            delete(
                    "/api/clubs/{clubId}/users/{userId}/parents/{parentId}",
                    club.getId(),
                    playerUser.getId(),
                    parentUser.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isNotFound());
  }

  // ========================
  // GET /api/clubs/{clubId}/users/{userId}/children
  // ========================

  @Test
  void listChildren_shouldReturnLinkedChildren() throws Exception {
    User parentUser = createUser("parent@test.com", ClubRole.PARENT, club);
    playerUser.getParents().add(parentUser);
    userRepository.saveAndFlush(playerUser);

    mockMvc
        .perform(
            get("/api/clubs/{clubId}/users/{userId}/children", club.getId(), parentUser.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(playerUser.getId().toString()));
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
