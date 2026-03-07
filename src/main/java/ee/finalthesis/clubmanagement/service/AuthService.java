package ee.finalthesis.clubmanagement.service;

import ee.finalthesis.clubmanagement.common.exception.BadRequestException;
import ee.finalthesis.clubmanagement.common.exception.ConflictException;
import ee.finalthesis.clubmanagement.common.exception.ResourceNotFoundException;
import ee.finalthesis.clubmanagement.config.SecurityProperties;
import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.repository.UserRepository;
import ee.finalthesis.clubmanagement.security.JwtTokenProvider;
import ee.finalthesis.clubmanagement.security.SecurityUtils;
import ee.finalthesis.clubmanagement.security.UserPrincipal;
import ee.finalthesis.clubmanagement.service.dto.auth.AuthResponseDTO;
import ee.finalthesis.clubmanagement.service.dto.auth.LoginRequestDTO;
import ee.finalthesis.clubmanagement.service.dto.auth.RegisterRequestDTO;
import ee.finalthesis.clubmanagement.service.dto.auth.UserDTO;
import ee.finalthesis.clubmanagement.service.mapper.UserMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtTokenProvider jwtTokenProvider;
  private final SecurityProperties securityProperties;
  private final UserMapper userMapper;

  @Transactional
  public UserDTO register(RegisterRequestDTO request) {
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new ConflictException("Email is already registered", "user", "emailExists");
    }

    User user =
        User.builder()
            .email(request.getEmail().toLowerCase())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .dateOfBirth(request.getDateOfBirth())
            .phone(request.getPhone())
            .active(true)
            .build();

    user = userRepository.save(user);
    return userMapper.toDto(user);
  }

  @Transactional(readOnly = true)
  public AuthResponseDTO login(LoginRequestDTO request) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

    UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
    String accessToken = jwtTokenProvider.generateAccessToken(principal);
    String refreshToken = jwtTokenProvider.generateRefreshToken(principal);

    return AuthResponseDTO.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .expiresIn(securityProperties.getTokenValidityInSeconds())
        .build();
  }

  @Transactional(readOnly = true)
  public AuthResponseDTO refreshToken(String refreshToken) {
    if (!jwtTokenProvider.validateToken(refreshToken)
        || !jwtTokenProvider.isRefreshToken(refreshToken)) {
      throw new BadRequestException("Invalid refresh token", "auth", "invalidRefreshToken");
    }

    UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

    if (!user.getActive()) {
      throw new BadRequestException("User account is deactivated", "auth", "accountDeactivated");
    }

    UserPrincipal principal = UserPrincipal.create(user);
    String newAccessToken = jwtTokenProvider.generateAccessToken(principal);
    String newRefreshToken = jwtTokenProvider.generateRefreshToken(principal);

    return AuthResponseDTO.builder()
        .accessToken(newAccessToken)
        .refreshToken(newRefreshToken)
        .expiresIn(securityProperties.getTokenValidityInSeconds())
        .build();
  }

  @Transactional(readOnly = true)
  public UserDTO getCurrentUser() {
    UUID userId =
        SecurityUtils.getCurrentUserId()
            .orElseThrow(
                () -> new BadRequestException("No authenticated user", "auth", "notAuthenticated"));

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

    return userMapper.toDto(user);
  }
}
