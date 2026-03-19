package ee.finalthesis.clubmanagement.api.controller;

import ee.finalthesis.clubmanagement.service.GameSquadService;
import ee.finalthesis.clubmanagement.service.dto.squad.AddSquadMembersDTO;
import ee.finalthesis.clubmanagement.service.dto.squad.SquadMemberDTO;
import ee.finalthesis.clubmanagement.service.dto.squad.SquadSummaryDTO;
import ee.finalthesis.clubmanagement.service.dto.squad.UpdateSquadMemberStatusDTO;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clubs/{clubId}/games/{gameId}/squad")
@RequiredArgsConstructor
public class GameSquadController {

  private final GameSquadService gameSquadService;

  @GetMapping
  @PreAuthorize("@clubSecurity.isAdminOrCoach(#clubId)")
  public ResponseEntity<List<SquadMemberDTO>> getSquadList(
      @PathVariable UUID clubId, @PathVariable UUID gameId) {
    return ResponseEntity.ok(gameSquadService.getSquadList(clubId, gameId));
  }

  @GetMapping("/summary")
  @PreAuthorize("@clubSecurity.isAdminOrCoach(#clubId)")
  public ResponseEntity<SquadSummaryDTO> getSquadSummary(
      @PathVariable UUID clubId, @PathVariable UUID gameId) {
    return ResponseEntity.ok(gameSquadService.getSquadSummary(clubId, gameId));
  }

  @PostMapping
  @PreAuthorize("@clubSecurity.isAdminOrCoach(#clubId)")
  public ResponseEntity<List<SquadMemberDTO>> addSquadMembers(
      @PathVariable UUID clubId,
      @PathVariable UUID gameId,
      @Valid @RequestBody AddSquadMembersDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(gameSquadService.addSquadMembers(clubId, gameId, request));
  }

  @PutMapping("/{userId}")
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<SquadMemberDTO> updateSquadMemberStatus(
      @PathVariable UUID clubId,
      @PathVariable UUID gameId,
      @PathVariable UUID userId,
      @Valid @RequestBody UpdateSquadMemberStatusDTO request) {
    return ResponseEntity.ok(
        gameSquadService.updateSquadMemberStatus(clubId, gameId, userId, request));
  }

  @DeleteMapping("/{userId}")
  @PreAuthorize("@clubSecurity.isAdminOrCoach(#clubId)")
  public ResponseEntity<Void> removeSquadMember(
      @PathVariable UUID clubId, @PathVariable UUID gameId, @PathVariable UUID userId) {
    gameSquadService.removeSquadMember(clubId, gameId, userId);
    return ResponseEntity.noContent().build();
  }
}
