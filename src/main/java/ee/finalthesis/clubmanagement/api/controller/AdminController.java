package ee.finalthesis.clubmanagement.api.controller;

import ee.finalthesis.clubmanagement.service.AdminService;
import ee.finalthesis.clubmanagement.service.dto.admin.AdminCreateUserDTO;
import ee.finalthesis.clubmanagement.service.dto.admin.AssignAdminDTO;
import ee.finalthesis.clubmanagement.service.dto.admin.CreateClubDTO;
import ee.finalthesis.clubmanagement.service.dto.auth.UserDTO;
import ee.finalthesis.clubmanagement.service.dto.club.ClubDTO;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

  private final AdminService adminService;

  @GetMapping("/clubs")
  public ResponseEntity<Page<ClubDTO>> listClubs(
      @RequestParam(required = false) String search, Pageable pageable) {
    return ResponseEntity.ok(adminService.listAllClubs(search, pageable));
  }

  @PostMapping("/clubs")
  public ResponseEntity<ClubDTO> createClub(@Valid @RequestBody CreateClubDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createClub(request));
  }

  @DeleteMapping("/clubs/{clubId}")
  public ResponseEntity<Void> deleteClub(@PathVariable UUID clubId) {
    adminService.deleteClub(clubId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/users")
  public ResponseEntity<Page<UserDTO>> listUsers(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) UUID clubId,
      @RequestParam(required = false) Boolean unaffiliated,
      Pageable pageable) {
    return ResponseEntity.ok(adminService.listAllUsers(search, clubId, unaffiliated, pageable));
  }

  @PostMapping("/users")
  public ResponseEntity<UserDTO> createUser(@Valid @RequestBody AdminCreateUserDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createUser(request));
  }

  @PostMapping("/clubs/{clubId}/admins")
  public ResponseEntity<UserDTO> assignClubAdmin(
      @PathVariable UUID clubId, @Valid @RequestBody AssignAdminDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(adminService.assignClubAdmin(clubId, request));
  }
}
