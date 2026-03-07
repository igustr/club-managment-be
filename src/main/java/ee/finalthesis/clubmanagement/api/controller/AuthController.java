package ee.finalthesis.clubmanagement.api.controller;

import ee.finalthesis.clubmanagement.service.AuthService;
import ee.finalthesis.clubmanagement.service.dto.auth.AuthResponseDTO;
import ee.finalthesis.clubmanagement.service.dto.auth.LoginRequestDTO;
import ee.finalthesis.clubmanagement.service.dto.auth.RefreshTokenRequestDTO;
import ee.finalthesis.clubmanagement.service.dto.auth.RegisterRequestDTO;
import ee.finalthesis.clubmanagement.service.dto.auth.UserDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<UserDTO> register(@Valid @RequestBody RegisterRequestDTO request) {
    UserDTO user = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(user);
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
    AuthResponseDTO response = authService.login(request);
    return ResponseEntity.ok(response);
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponseDTO> refresh(
      @Valid @RequestBody RefreshTokenRequestDTO request) {
    AuthResponseDTO response = authService.refreshToken(request.getRefreshToken());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/me")
  public ResponseEntity<UserDTO> getCurrentUser() {
    UserDTO user = authService.getCurrentUser();
    return ResponseEntity.ok(user);
  }
}
