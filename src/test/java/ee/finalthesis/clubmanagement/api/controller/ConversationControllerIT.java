package ee.finalthesis.clubmanagement.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.finalthesis.clubmanagement.IntegrationTest;
import ee.finalthesis.clubmanagement.domain.Club;
import ee.finalthesis.clubmanagement.domain.Conversation;
import ee.finalthesis.clubmanagement.domain.ConversationParticipant;
import ee.finalthesis.clubmanagement.domain.ConversationReadStatus;
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
import ee.finalthesis.clubmanagement.service.dto.chat.CreateDirectConversationDTO;
import ee.finalthesis.clubmanagement.service.dto.chat.SendMessageDTO;
import java.time.LocalDate;
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
class ConversationControllerIT {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper om;
  @Autowired private ClubRepository clubRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private TeamRepository teamRepository;
  @Autowired private TeamMemberRepository teamMemberRepository;
  @Autowired private TrainingSessionRepository trainingSessionRepository;
  @Autowired private AttendanceRepository attendanceRepository;
  @Autowired private ConversationRepository conversationRepository;
  @Autowired private ConversationParticipantRepository conversationParticipantRepository;
  @Autowired private MessageRepository messageRepository;
  @Autowired private ConversationReadStatusRepository conversationReadStatusRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtTokenProvider jwtTokenProvider;

  private Club club;
  private Club otherClub;
  private User adminUser;
  private User coachUser;
  private User playerUser;
  private User parentUser;
  private User otherClubUser;
  private Team team;
  private String adminToken;
  private String coachToken;
  private String playerToken;
  private String parentToken;
  private String otherClubToken;

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
    playerUser = createUser("player@test.com", ClubRole.PLAYER, club);
    parentUser = createUser("parent@test.com", ClubRole.PARENT, club);
    otherClubUser = createUser("other@test.com", ClubRole.ADMIN, otherClub);

    // Set up parent-child relationship: parentUser is parent of playerUser
    playerUser.setParents(new HashSet<>(Set.of(parentUser)));
    playerUser = userRepository.saveAndFlush(playerUser);

    // Create team and add members directly (not via API, so no auto-conversation)
    team =
        teamRepository.saveAndFlush(
            Team.builder().name("U-19").ageGroup("U19").season("2026").club(club).build());

    // Create team conversation manually (simulating what TeamService.createTeam does)
    Conversation teamConversation =
        conversationRepository.saveAndFlush(
            Conversation.builder().type(ConversationType.TEAM).team(team).club(club).build());

    // Add team members and conversation participants
    addTeamMemberAndParticipant(team, coachUser, teamConversation);
    addTeamMemberAndParticipant(team, playerUser, teamConversation);

    // Also add parent to team conversation (simulating addParticipantToTeamConversation logic)
    addConversationParticipant(teamConversation, parentUser);

    adminToken = generateToken(adminUser);
    coachToken = generateToken(coachUser);
    playerToken = generateToken(playerUser);
    parentToken = generateToken(parentUser);
    otherClubToken = generateToken(otherClubUser);
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
    userRepository.deleteAll();
    clubRepository.deleteAll();
  }

  // ========================
  // GET /api/clubs/{clubId}/conversations
  // ========================

  @Test
  void listConversations_asPlayer_shouldReturnTeamConversation() throws Exception {
    mockMvc
        .perform(
            get("/api/clubs/{clubId}/conversations", club.getId())
                .header("Authorization", "Bearer " + playerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].type").value("TEAM"))
        .andExpect(jsonPath("$[0].name").value("U-19"));
  }

  @Test
  void listConversations_asParent_shouldReturnTeamConversation() throws Exception {
    mockMvc
        .perform(
            get("/api/clubs/{clubId}/conversations", club.getId())
                .header("Authorization", "Bearer " + parentToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].type").value("TEAM"));
  }

  @Test
  void listConversations_asAdmin_noConversations_shouldReturnEmpty() throws Exception {
    // Admin is not a participant of any conversation
    mockMvc
        .perform(
            get("/api/clubs/{clubId}/conversations", club.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void listConversations_unauthenticated_shouldReturn401() throws Exception {
    mockMvc
        .perform(get("/api/clubs/{clubId}/conversations", club.getId()))
        .andExpect(status().isUnauthorized());
  }

  // ========================
  // POST /api/clubs/{clubId}/conversations (create direct)
  // ========================

  @Test
  void createDirectConversation_shouldCreateConversation() throws Exception {
    CreateDirectConversationDTO request = new CreateDirectConversationDTO();
    request.setParticipantId(coachUser.getId());

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/conversations", club.getId())
                .header("Authorization", "Bearer " + playerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("DIRECT"))
        .andExpect(jsonPath("$.participants.length()").value(2));
  }

  @Test
  void createDirectConversation_duplicate_shouldReturnExisting() throws Exception {
    CreateDirectConversationDTO request = new CreateDirectConversationDTO();
    request.setParticipantId(coachUser.getId());

    // Create first
    mockMvc
        .perform(
            post("/api/clubs/{clubId}/conversations", club.getId())
                .header("Authorization", "Bearer " + playerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isCreated());

    // Create again — should return the same conversation (still 201 from controller, but same ID)
    mockMvc
        .perform(
            post("/api/clubs/{clubId}/conversations", club.getId())
                .header("Authorization", "Bearer " + playerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.type").value("DIRECT"))
        .andExpect(jsonPath("$.participants.length()").value(2));

    // Verify only one direct conversation exists
    List<Conversation> directs =
        conversationRepository.findAll().stream()
            .filter(c -> c.getType() == ConversationType.DIRECT)
            .toList();
    assertThat(directs).hasSize(1);
  }

  @Test
  void createDirectConversation_withSelf_shouldReturn400() throws Exception {
    CreateDirectConversationDTO request = new CreateDirectConversationDTO();
    request.setParticipantId(playerUser.getId());

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/conversations", club.getId())
                .header("Authorization", "Bearer " + playerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void createDirectConversation_withUserNotInClub_shouldReturn400() throws Exception {
    CreateDirectConversationDTO request = new CreateDirectConversationDTO();
    request.setParticipantId(otherClubUser.getId());

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/conversations", club.getId())
                .header("Authorization", "Bearer " + playerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }

  // ========================
  // GET /api/clubs/{clubId}/conversations/{conversationId}
  // ========================

  @Test
  void getConversation_asParticipant_shouldReturn200() throws Exception {
    Conversation conversation = conversationRepository.findByTeamId(team.getId()).orElseThrow();

    mockMvc
        .perform(
            get(
                    "/api/clubs/{clubId}/conversations/{conversationId}",
                    club.getId(),
                    conversation.getId())
                .header("Authorization", "Bearer " + playerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(conversation.getId().toString()))
        .andExpect(jsonPath("$.type").value("TEAM"))
        .andExpect(jsonPath("$.name").value("U-19"));
  }

  @Test
  void getConversation_asNonParticipant_shouldReturn403() throws Exception {
    Conversation conversation = conversationRepository.findByTeamId(team.getId()).orElseThrow();

    // Admin is not a participant
    mockMvc
        .perform(
            get(
                    "/api/clubs/{clubId}/conversations/{conversationId}",
                    club.getId(),
                    conversation.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isForbidden());
  }

  @Test
  void getConversation_notFound_shouldReturn404() throws Exception {
    mockMvc
        .perform(
            get(
                    "/api/clubs/{clubId}/conversations/{conversationId}",
                    club.getId(),
                    UUID.randomUUID())
                .header("Authorization", "Bearer " + playerToken))
        .andExpect(status().isNotFound());
  }

  // ========================
  // GET /api/clubs/{clubId}/conversations/{conversationId}/messages
  // ========================

  @Test
  void getMessages_asParticipant_shouldReturnPaginatedMessages() throws Exception {
    Conversation conversation = conversationRepository.findByTeamId(team.getId()).orElseThrow();

    // Send a message first
    SendMessageDTO sendRequest = new SendMessageDTO();
    sendRequest.setText("Hello team!");

    mockMvc
        .perform(
            post(
                    "/api/clubs/{clubId}/conversations/{conversationId}/messages",
                    club.getId(),
                    conversation.getId())
                .header("Authorization", "Bearer " + coachToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(sendRequest)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            get(
                    "/api/clubs/{clubId}/conversations/{conversationId}/messages",
                    club.getId(),
                    conversation.getId())
                .header("Authorization", "Bearer " + playerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(1))
        .andExpect(jsonPath("$.content[0].text").value("Hello team!"))
        .andExpect(jsonPath("$.content[0].senderFirstName").value("Test"));
  }

  @Test
  void getMessages_asNonParticipant_shouldReturn403() throws Exception {
    Conversation conversation = conversationRepository.findByTeamId(team.getId()).orElseThrow();

    mockMvc
        .perform(
            get(
                    "/api/clubs/{clubId}/conversations/{conversationId}/messages",
                    club.getId(),
                    conversation.getId())
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isForbidden());
  }

  // ========================
  // POST /api/clubs/{clubId}/conversations/{conversationId}/messages
  // ========================

  @Test
  void sendMessage_asParticipant_shouldCreateMessage() throws Exception {
    Conversation conversation = conversationRepository.findByTeamId(team.getId()).orElseThrow();

    SendMessageDTO request = new SendMessageDTO();
    request.setText("Training at 18:00");

    mockMvc
        .perform(
            post(
                    "/api/clubs/{clubId}/conversations/{conversationId}/messages",
                    club.getId(),
                    conversation.getId())
                .header("Authorization", "Bearer " + coachToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.text").value("Training at 18:00"))
        .andExpect(jsonPath("$.senderId").value(coachUser.getId().toString()));

    // Verify conversation updated with last message
    Conversation updated = conversationRepository.findById(conversation.getId()).orElseThrow();
    assertThat(updated.getLastMessageText()).isEqualTo("Training at 18:00");
    assertThat(updated.getLastMessageSender().getId()).isEqualTo(coachUser.getId());
  }

  @Test
  void sendMessage_asNonParticipant_shouldReturn403() throws Exception {
    Conversation conversation = conversationRepository.findByTeamId(team.getId()).orElseThrow();

    SendMessageDTO request = new SendMessageDTO();
    request.setText("Hello");

    mockMvc
        .perform(
            post(
                    "/api/clubs/{clubId}/conversations/{conversationId}/messages",
                    club.getId(),
                    conversation.getId())
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  void sendMessage_shouldIncrementUnreadCountForOthers() throws Exception {
    Conversation conversation = conversationRepository.findByTeamId(team.getId()).orElseThrow();

    SendMessageDTO request = new SendMessageDTO();
    request.setText("Important update");

    mockMvc
        .perform(
            post(
                    "/api/clubs/{clubId}/conversations/{conversationId}/messages",
                    club.getId(),
                    conversation.getId())
                .header("Authorization", "Bearer " + coachToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isCreated());

    // Player's unread count should be 1
    ConversationReadStatus playerStatus =
        conversationReadStatusRepository
            .findByConversationIdAndUserId(conversation.getId(), playerUser.getId())
            .orElseThrow();
    assertThat(playerStatus.getUnreadCount()).isEqualTo(1);

    // Coach (sender) unread count should remain 0
    ConversationReadStatus coachStatus =
        conversationReadStatusRepository
            .findByConversationIdAndUserId(conversation.getId(), coachUser.getId())
            .orElseThrow();
    assertThat(coachStatus.getUnreadCount()).isEqualTo(0);
  }

  // ========================
  // PUT /api/clubs/{clubId}/conversations/{conversationId}/read
  // ========================

  @Test
  void markAsRead_shouldResetUnreadCount() throws Exception {
    Conversation conversation = conversationRepository.findByTeamId(team.getId()).orElseThrow();

    // Send a message to create unread
    SendMessageDTO request = new SendMessageDTO();
    request.setText("New message");

    mockMvc
        .perform(
            post(
                    "/api/clubs/{clubId}/conversations/{conversationId}/messages",
                    club.getId(),
                    conversation.getId())
                .header("Authorization", "Bearer " + coachToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isCreated());

    // Verify player has unread
    ConversationReadStatus beforeRead =
        conversationReadStatusRepository
            .findByConversationIdAndUserId(conversation.getId(), playerUser.getId())
            .orElseThrow();
    assertThat(beforeRead.getUnreadCount()).isEqualTo(1);

    // Mark as read
    mockMvc
        .perform(
            put(
                    "/api/clubs/{clubId}/conversations/{conversationId}/read",
                    club.getId(),
                    conversation.getId())
                .header("Authorization", "Bearer " + playerToken))
        .andExpect(status().isNoContent());

    // Verify unread reset
    ConversationReadStatus afterRead =
        conversationReadStatusRepository
            .findByConversationIdAndUserId(conversation.getId(), playerUser.getId())
            .orElseThrow();
    assertThat(afterRead.getUnreadCount()).isEqualTo(0);
    assertThat(afterRead.getLastReadAt()).isNotNull();
  }

  // ========================
  // GET /api/clubs/{clubId}/conversations/unread-count
  // ========================

  @Test
  void getUnreadCount_shouldReturnTotalUnreadCount() throws Exception {
    Conversation conversation = conversationRepository.findByTeamId(team.getId()).orElseThrow();

    // Send 2 messages
    SendMessageDTO request = new SendMessageDTO();
    request.setText("Message 1");
    mockMvc
        .perform(
            post(
                    "/api/clubs/{clubId}/conversations/{conversationId}/messages",
                    club.getId(),
                    conversation.getId())
                .header("Authorization", "Bearer " + coachToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isCreated());

    request.setText("Message 2");
    mockMvc
        .perform(
            post(
                    "/api/clubs/{clubId}/conversations/{conversationId}/messages",
                    club.getId(),
                    conversation.getId())
                .header("Authorization", "Bearer " + coachToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isCreated());

    mockMvc
        .perform(
            get("/api/clubs/{clubId}/conversations/unread-count", club.getId())
                .header("Authorization", "Bearer " + playerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unreadCount").value(2));
  }

  @Test
  void getUnreadCount_noUnread_shouldReturnZero() throws Exception {
    mockMvc
        .perform(
            get("/api/clubs/{clubId}/conversations/unread-count", club.getId())
                .header("Authorization", "Bearer " + playerToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unreadCount").value(0));
  }

  // ========================
  // Team integration tests
  // ========================

  @Test
  void teamMemberAdded_shouldBeAddedToTeamConversation() throws Exception {
    Conversation conversation = conversationRepository.findByTeamId(team.getId()).orElseThrow();

    // Verify coach, player, and parent are participants
    List<ConversationParticipant> participants =
        conversationParticipantRepository.findByConversationId(conversation.getId());

    assertThat(participants).hasSize(3); // coach, player, parent
    List<UUID> participantUserIds = participants.stream().map(p -> p.getUser().getId()).toList();
    assertThat(participantUserIds)
        .contains(coachUser.getId(), playerUser.getId(), parentUser.getId());
  }

  @Test
  void directConversation_name_shouldBeOtherParticipantName() throws Exception {
    CreateDirectConversationDTO request = new CreateDirectConversationDTO();
    request.setParticipantId(coachUser.getId());

    mockMvc
        .perform(
            post("/api/clubs/{clubId}/conversations", club.getId())
                .header("Authorization", "Bearer " + playerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Test User"));
  }

  // ========================
  // Helper methods
  // ========================

  private void addTeamMemberAndParticipant(Team t, User user, Conversation conversation) {
    teamMemberRepository.saveAndFlush(
        TeamMember.builder().team(t).user(user).joinedDate(LocalDate.now()).build());
    addConversationParticipant(conversation, user);
  }

  private void addConversationParticipant(Conversation conversation, User user) {
    conversationParticipantRepository.saveAndFlush(
        ConversationParticipant.builder().conversation(conversation).user(user).build());
    conversationReadStatusRepository.saveAndFlush(
        ConversationReadStatus.builder()
            .conversation(conversation)
            .user(user)
            .unreadCount(0)
            .build());
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
