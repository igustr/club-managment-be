package ee.finalthesis.clubmanagement.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.finalthesis.clubmanagement.IntegrationTest;
import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.repository.AttendanceRepository;
import ee.finalthesis.clubmanagement.repository.ConversationParticipantRepository;
import ee.finalthesis.clubmanagement.repository.ConversationReadStatusRepository;
import ee.finalthesis.clubmanagement.repository.ConversationRepository;
import ee.finalthesis.clubmanagement.repository.MessageRepository;
import ee.finalthesis.clubmanagement.repository.TeamMemberRepository;
import ee.finalthesis.clubmanagement.repository.TrainingSessionRepository;
import ee.finalthesis.clubmanagement.repository.UserRepository;
import ee.finalthesis.clubmanagement.service.dto.auth.AuthResponseDTO;
import ee.finalthesis.clubmanagement.service.dto.auth.LoginRequestDTO;
import ee.finalthesis.clubmanagement.service.dto.auth.RefreshTokenRequestDTO;
import ee.finalthesis.clubmanagement.service.dto.auth.RegisterRequestDTO;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@IntegrationTest
@AutoConfigureMockMvc
class AuthControllerIT {

  private static final String DEFAULT_EMAIL = "test@example.com";
  private static final String DEFAULT_PASSWORD = "password123";
  private static final String DEFAULT_FIRST_NAME = "Test";
  private static final String DEFAULT_LAST_NAME = "User";
  private static final LocalDate DEFAULT_DOB = LocalDate.of(2000, 1, 15);
  private static final String DEFAULT_PHONE = "+372 5551234";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper om;
  @Autowired private AttendanceRepository attendanceRepository;
  @Autowired private ConversationReadStatusRepository conversationReadStatusRepository;
  @Autowired private MessageRepository messageRepository;
  @Autowired private ConversationParticipantRepository conversationParticipantRepository;
  @Autowired private ConversationRepository conversationRepository;
  @Autowired private TrainingSessionRepository trainingSessionRepository;
  @Autowired private TeamMemberRepository teamMemberRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private User testUser;

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
  }

  // ========================
  // POST /api/auth/register
  // ========================

  @Test
  void register_shouldCreateUser() throws Exception {
    RegisterRequestDTO request = createRegisterRequest();

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value(DEFAULT_EMAIL))
        .andExpect(jsonPath("$.firstName").value(DEFAULT_FIRST_NAME))
        .andExpect(jsonPath("$.lastName").value(DEFAULT_LAST_NAME))
        .andExpect(jsonPath("$.phone").value(DEFAULT_PHONE))
        .andExpect(jsonPath("$.active").value(true))
        .andExpect(jsonPath("$.role").doesNotExist())
        .andExpect(jsonPath("$.clubId").doesNotExist())
        .andExpect(jsonPath("$.id").exists());

    assertThat(userRepository.findByEmail(DEFAULT_EMAIL)).isPresent();
  }

  @Test
  void register_shouldRejectDuplicateEmail() throws Exception {
    createAndPersistUser();

    RegisterRequestDTO request = createRegisterRequest();

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isConflict());
  }

  @Test
  void register_shouldRejectInvalidEmail() throws Exception {
    RegisterRequestDTO request = createRegisterRequest();
    request.setEmail("not-an-email");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void register_shouldRejectBlankPassword() throws Exception {
    RegisterRequestDTO request = createRegisterRequest();
    request.setPassword("");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void register_shouldRejectShortPassword() throws Exception {
    RegisterRequestDTO request = createRegisterRequest();
    request.setPassword("short");

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void register_shouldRejectMissingFirstName() throws Exception {
    RegisterRequestDTO request = createRegisterRequest();
    request.setFirstName(null);

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }

  // ========================
  // POST /api/auth/login
  // ========================

  @Test
  void login_shouldReturnTokens() throws Exception {
    createAndPersistUser();

    LoginRequestDTO request = new LoginRequestDTO();
    request.setEmail(DEFAULT_EMAIL);
    request.setPassword(DEFAULT_PASSWORD);

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").isNumber());
  }

  @Test
  void login_shouldRejectWrongPassword() throws Exception {
    createAndPersistUser();

    LoginRequestDTO request = new LoginRequestDTO();
    request.setEmail(DEFAULT_EMAIL);
    request.setPassword("wrongpassword");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void login_shouldRejectNonexistentUser() throws Exception {
    LoginRequestDTO request = new LoginRequestDTO();
    request.setEmail("nobody@example.com");
    request.setPassword(DEFAULT_PASSWORD);

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void login_shouldRejectBlankEmail() throws Exception {
    LoginRequestDTO request = new LoginRequestDTO();
    request.setEmail("");
    request.setPassword(DEFAULT_PASSWORD);

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }

  // ========================
  // POST /api/auth/refresh
  // ========================

  @Test
  void refresh_shouldReturnNewTokens() throws Exception {
    createAndPersistUser();
    String refreshToken = loginAndGetRefreshToken();

    RefreshTokenRequestDTO request = new RefreshTokenRequestDTO();
    request.setRefreshToken(refreshToken);

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").isNotEmpty())
        .andExpect(jsonPath("$.refreshToken").isNotEmpty())
        .andExpect(jsonPath("$.tokenType").value("Bearer"));
  }

  @Test
  void refresh_shouldRejectInvalidToken() throws Exception {
    RefreshTokenRequestDTO request = new RefreshTokenRequestDTO();
    request.setRefreshToken("invalid.token.value");

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void refresh_shouldRejectAccessTokenAsRefresh() throws Exception {
    createAndPersistUser();
    String accessToken = loginAndGetAccessToken();

    RefreshTokenRequestDTO request = new RefreshTokenRequestDTO();
    request.setRefreshToken(accessToken);

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void refresh_shouldRejectBlankToken() throws Exception {
    RefreshTokenRequestDTO request = new RefreshTokenRequestDTO();
    request.setRefreshToken("");

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsBytes(request)))
        .andExpect(status().isBadRequest());
  }

  // ========================
  // GET /api/auth/me
  // ========================

  @Test
  void me_shouldReturnCurrentUser() throws Exception {
    createAndPersistUser();
    String accessToken = loginAndGetAccessToken();

    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(DEFAULT_EMAIL))
        .andExpect(jsonPath("$.firstName").value(DEFAULT_FIRST_NAME))
        .andExpect(jsonPath("$.lastName").value(DEFAULT_LAST_NAME))
        .andExpect(jsonPath("$.id").exists());
  }

  @Test
  void me_shouldRejectUnauthenticated() throws Exception {
    mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void me_shouldRejectInvalidToken() throws Exception {
    mockMvc
        .perform(get("/api/auth/me").header("Authorization", "Bearer invalid.token.here"))
        .andExpect(status().isUnauthorized());
  }

  // ========================
  // Helper methods
  // ========================

  private RegisterRequestDTO createRegisterRequest() {
    RegisterRequestDTO request = new RegisterRequestDTO();
    request.setEmail(DEFAULT_EMAIL);
    request.setPassword(DEFAULT_PASSWORD);
    request.setFirstName(DEFAULT_FIRST_NAME);
    request.setLastName(DEFAULT_LAST_NAME);
    request.setDateOfBirth(DEFAULT_DOB);
    request.setPhone(DEFAULT_PHONE);
    return request;
  }

  private void createAndPersistUser() {
    testUser =
        User.builder()
            .email(DEFAULT_EMAIL)
            .passwordHash(passwordEncoder.encode(DEFAULT_PASSWORD))
            .firstName(DEFAULT_FIRST_NAME)
            .lastName(DEFAULT_LAST_NAME)
            .dateOfBirth(DEFAULT_DOB)
            .phone(DEFAULT_PHONE)
            .active(true)
            .build();
    testUser = userRepository.saveAndFlush(testUser);
  }

  private String loginAndGetAccessToken() throws Exception {
    LoginRequestDTO loginRequest = new LoginRequestDTO();
    loginRequest.setEmail(DEFAULT_EMAIL);
    loginRequest.setPassword(DEFAULT_PASSWORD);

    MvcResult result =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();

    AuthResponseDTO response =
        om.readValue(result.getResponse().getContentAsString(), AuthResponseDTO.class);
    return response.getAccessToken();
  }

  private String loginAndGetRefreshToken() throws Exception {
    LoginRequestDTO loginRequest = new LoginRequestDTO();
    loginRequest.setEmail(DEFAULT_EMAIL);
    loginRequest.setPassword(DEFAULT_PASSWORD);

    MvcResult result =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsBytes(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();

    AuthResponseDTO response =
        om.readValue(result.getResponse().getContentAsString(), AuthResponseDTO.class);
    return response.getRefreshToken();
  }
}
