package ee.finalthesis.clubmanagement.api.controller;

import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.service.UserService;
import ee.finalthesis.clubmanagement.service.dto.auth.UserDTO;
import ee.finalthesis.clubmanagement.service.dto.user.AddUserToClubDTO;
import ee.finalthesis.clubmanagement.service.dto.user.LinkParentDTO;
import ee.finalthesis.clubmanagement.service.dto.user.UpdateUserDTO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @GetMapping("/clubs/{clubId}/users")
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<Page<UserDTO>> listClubUsers(
      @PathVariable UUID clubId,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) ClubRole role,
      Pageable pageable) {
    return ResponseEntity.ok(userService.listClubUsers(clubId, search, role, pageable));
  }

  @PostMapping("/clubs/{clubId}/users")
  @PreAuthorize("@clubSecurity.isAdmin(#clubId)")
  public ResponseEntity<UserDTO> addUserToClub(
      @PathVariable UUID clubId, @Valid @RequestBody AddUserToClubDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(userService.addUserToClub(clubId, request));
  }

  @GetMapping("/clubs/{clubId}/users/{userId}")
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<UserDTO> getClubUser(@PathVariable UUID clubId, @PathVariable UUID userId) {
    return ResponseEntity.ok(userService.getClubUser(clubId, userId));
  }

  @PutMapping("/clubs/{clubId}/users/{userId}")
  @PreAuthorize("@clubSecurity.isAdmin(#clubId)")
  public ResponseEntity<UserDTO> updateUser(
      @PathVariable UUID clubId,
      @PathVariable UUID userId,
      @Valid @RequestBody UpdateUserDTO request) {
    return ResponseEntity.ok(userService.updateUser(clubId, userId, request));
  }

  @DeleteMapping("/clubs/{clubId}/users/{userId}")
  @PreAuthorize("@clubSecurity.isAdmin(#clubId)")
  public ResponseEntity<Void> removeUserFromClub(
      @PathVariable UUID clubId, @PathVariable UUID userId) {
    userService.removeUserFromClub(clubId, userId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/users/unaffiliated")
  @PreAuthorize("@clubSecurity.isAnyAdmin()")
  public ResponseEntity<Page<UserDTO>> listUnaffiliatedUsers(
      @RequestParam(required = false) String search, Pageable pageable) {
    return ResponseEntity.ok(userService.listUnaffiliatedUsers(search, pageable));
  }

  // ========================
  // Parent-child endpoints
  // ========================

  @GetMapping("/clubs/{clubId}/users/{userId}/parents")
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<List<UserDTO>> listParents(
      @PathVariable UUID clubId, @PathVariable UUID userId) {
    return ResponseEntity.ok(userService.listParents(clubId, userId));
  }

  @PostMapping("/clubs/{clubId}/users/{userId}/parents")
  @PreAuthorize("@clubSecurity.isAdmin(#clubId)")
  public ResponseEntity<UserDTO> linkParent(
      @PathVariable UUID clubId,
      @PathVariable UUID userId,
      @Valid @RequestBody LinkParentDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(userService.linkParent(clubId, userId, request));
  }

  @DeleteMapping("/clubs/{clubId}/users/{userId}/parents/{parentId}")
  @PreAuthorize("@clubSecurity.isAdmin(#clubId)")
  public ResponseEntity<Void> unlinkParent(
      @PathVariable UUID clubId, @PathVariable UUID userId, @PathVariable UUID parentId) {
    userService.unlinkParent(clubId, userId, parentId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/clubs/{clubId}/users/{userId}/children")
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<List<UserDTO>> listChildren(
      @PathVariable UUID clubId, @PathVariable UUID userId) {
    return ResponseEntity.ok(userService.listChildren(clubId, userId));
  }
}
