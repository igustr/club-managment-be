package ee.finalthesis.clubmanagement.api.controller;

import ee.finalthesis.clubmanagement.service.TournamentService;
import ee.finalthesis.clubmanagement.service.dto.tournament.CreateTournamentDTO;
import ee.finalthesis.clubmanagement.service.dto.tournament.TournamentDTO;
import ee.finalthesis.clubmanagement.service.dto.tournament.UpdateTournamentDTO;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clubs/{clubId}")
@RequiredArgsConstructor
public class TournamentController {

  private final TournamentService tournamentService;

  @GetMapping("/tournaments")
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<List<TournamentDTO>> listTournaments(
      @PathVariable UUID clubId, @RequestParam(defaultValue = "false") boolean myTeams) {
    return ResponseEntity.ok(tournamentService.listTournamentsByClub(clubId, myTeams));
  }

  @GetMapping("/tournaments/{tournamentId}")
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<TournamentDTO> getTournament(
      @PathVariable UUID clubId, @PathVariable UUID tournamentId) {
    return ResponseEntity.ok(tournamentService.getTournament(clubId, tournamentId));
  }

  @PostMapping("/teams/{teamId}/tournaments")
  @PreAuthorize("@teamSecurity.canManageTeam(#teamId)")
  public ResponseEntity<TournamentDTO> createTournament(
      @PathVariable UUID clubId,
      @PathVariable UUID teamId,
      @Valid @RequestBody CreateTournamentDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(tournamentService.createTournament(clubId, teamId, request));
  }

  @PutMapping("/tournaments/{tournamentId}")
  @PreAuthorize("@clubSecurity.isAdminOrCoach(#clubId)")
  public ResponseEntity<TournamentDTO> updateTournament(
      @PathVariable UUID clubId,
      @PathVariable UUID tournamentId,
      @Valid @RequestBody UpdateTournamentDTO request) {
    return ResponseEntity.ok(tournamentService.updateTournament(clubId, tournamentId, request));
  }

  @PutMapping("/tournaments/{tournamentId}/cancel")
  @PreAuthorize("@clubSecurity.isAdminOrCoach(#clubId)")
  public ResponseEntity<Void> cancelTournament(
      @PathVariable UUID clubId, @PathVariable UUID tournamentId) {
    tournamentService.cancelTournament(clubId, tournamentId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/tournaments/{tournamentId}")
  @PreAuthorize("@clubSecurity.isAdmin(#clubId)")
  public ResponseEntity<Void> deleteTournament(
      @PathVariable UUID clubId, @PathVariable UUID tournamentId) {
    tournamentService.deleteTournament(clubId, tournamentId);
    return ResponseEntity.noContent().build();
  }
}
