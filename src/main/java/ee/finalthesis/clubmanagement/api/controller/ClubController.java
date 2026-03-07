package ee.finalthesis.clubmanagement.api.controller;

import ee.finalthesis.clubmanagement.service.ClubService;
import ee.finalthesis.clubmanagement.service.dto.club.ClubDTO;
import ee.finalthesis.clubmanagement.service.dto.club.UpdateClubDTO;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clubs/{clubId}")
@RequiredArgsConstructor
public class ClubController {

  private final ClubService clubService;

  @GetMapping
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<ClubDTO> getClub(@PathVariable UUID clubId) {
    return ResponseEntity.ok(clubService.getClub(clubId));
  }

  @PutMapping
  @PreAuthorize("@clubSecurity.isAdmin(#clubId)")
  public ResponseEntity<ClubDTO> updateClub(
      @PathVariable UUID clubId, @Valid @RequestBody UpdateClubDTO request) {
    return ResponseEntity.ok(clubService.updateClub(clubId, request));
  }
}
