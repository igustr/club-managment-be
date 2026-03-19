package ee.finalthesis.clubmanagement.api.controller;

import ee.finalthesis.clubmanagement.service.GameService;
import ee.finalthesis.clubmanagement.service.dto.game.CreateGameDTO;
import ee.finalthesis.clubmanagement.service.dto.game.GameDTO;
import ee.finalthesis.clubmanagement.service.dto.game.UpdateGameDTO;
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
public class GameController {

  private final GameService gameService;

  @GetMapping("/games")
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<List<GameDTO>> listGames(
      @PathVariable UUID clubId, @RequestParam(defaultValue = "false") boolean myTeams) {
    return ResponseEntity.ok(gameService.listGamesByClub(clubId, myTeams));
  }

  @GetMapping("/games/{gameId}")
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<GameDTO> getGame(@PathVariable UUID clubId, @PathVariable UUID gameId) {
    return ResponseEntity.ok(gameService.getGame(clubId, gameId));
  }

  @PostMapping("/teams/{teamId}/games")
  @PreAuthorize("@teamSecurity.canManageTeam(#teamId)")
  public ResponseEntity<GameDTO> createGame(
      @PathVariable UUID clubId,
      @PathVariable UUID teamId,
      @Valid @RequestBody CreateGameDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(gameService.createGame(clubId, teamId, request));
  }

  @PutMapping("/games/{gameId}")
  @PreAuthorize("@clubSecurity.isAdminOrCoach(#clubId)")
  public ResponseEntity<GameDTO> updateGame(
      @PathVariable UUID clubId,
      @PathVariable UUID gameId,
      @Valid @RequestBody UpdateGameDTO request) {
    return ResponseEntity.ok(gameService.updateGame(clubId, gameId, request));
  }

  @PutMapping("/games/{gameId}/cancel")
  @PreAuthorize("@clubSecurity.isAdminOrCoach(#clubId)")
  public ResponseEntity<Void> cancelGame(@PathVariable UUID clubId, @PathVariable UUID gameId) {
    gameService.cancelGame(clubId, gameId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/games/{gameId}")
  @PreAuthorize("@clubSecurity.isAdmin(#clubId)")
  public ResponseEntity<Void> deleteGame(@PathVariable UUID clubId, @PathVariable UUID gameId) {
    gameService.deleteGame(clubId, gameId);
    return ResponseEntity.noContent().build();
  }
}
