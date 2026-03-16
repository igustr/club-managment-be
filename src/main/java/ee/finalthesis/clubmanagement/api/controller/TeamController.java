package ee.finalthesis.clubmanagement.api.controller;

import ee.finalthesis.clubmanagement.service.TeamService;
import ee.finalthesis.clubmanagement.service.dto.team.AddTeamMemberDTO;
import ee.finalthesis.clubmanagement.service.dto.team.CreateTeamDTO;
import ee.finalthesis.clubmanagement.service.dto.team.TeamDTO;
import ee.finalthesis.clubmanagement.service.dto.team.TeamMemberDTO;
import ee.finalthesis.clubmanagement.service.dto.team.UpdateTeamDTO;
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
@RequestMapping("/api/clubs/{clubId}/teams")
@RequiredArgsConstructor
public class TeamController {

  private final TeamService teamService;

  @GetMapping
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<List<TeamDTO>> listTeams(
      @PathVariable UUID clubId, @RequestParam(defaultValue = "false") boolean myTeams) {
    return ResponseEntity.ok(teamService.listTeams(clubId, myTeams));
  }

  @PostMapping
  @PreAuthorize("@clubSecurity.isAdmin(#clubId)")
  public ResponseEntity<TeamDTO> createTeam(
      @PathVariable UUID clubId, @Valid @RequestBody CreateTeamDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(teamService.createTeam(clubId, request));
  }

  @GetMapping("/{teamId}")
  @PreAuthorize("@teamSecurity.canAccessTeam(#teamId)")
  public ResponseEntity<TeamDTO> getTeam(@PathVariable UUID clubId, @PathVariable UUID teamId) {
    return ResponseEntity.ok(teamService.getTeam(clubId, teamId));
  }

  @PutMapping("/{teamId}")
  @PreAuthorize("@clubSecurity.isAdmin(#clubId)")
  public ResponseEntity<TeamDTO> updateTeam(
      @PathVariable UUID clubId,
      @PathVariable UUID teamId,
      @Valid @RequestBody UpdateTeamDTO request) {
    return ResponseEntity.ok(teamService.updateTeam(clubId, teamId, request));
  }

  @DeleteMapping("/{teamId}")
  @PreAuthorize("@clubSecurity.isAdmin(#clubId)")
  public ResponseEntity<Void> deleteTeam(@PathVariable UUID clubId, @PathVariable UUID teamId) {
    teamService.deleteTeam(clubId, teamId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{teamId}/members")
  @PreAuthorize("@teamSecurity.canAccessTeam(#teamId)")
  public ResponseEntity<List<TeamMemberDTO>> listTeamMembers(
      @PathVariable UUID clubId, @PathVariable UUID teamId) {
    return ResponseEntity.ok(teamService.listTeamMembers(clubId, teamId));
  }

  @PostMapping("/{teamId}/members")
  @PreAuthorize("@teamSecurity.canManageTeam(#teamId)")
  public ResponseEntity<TeamMemberDTO> addTeamMember(
      @PathVariable UUID clubId,
      @PathVariable UUID teamId,
      @Valid @RequestBody AddTeamMemberDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(teamService.addTeamMember(clubId, teamId, request));
  }

  @DeleteMapping("/{teamId}/members/{userId}")
  @PreAuthorize("@teamSecurity.canManageTeam(#teamId)")
  public ResponseEntity<Void> removeTeamMember(
      @PathVariable UUID clubId, @PathVariable UUID teamId, @PathVariable UUID userId) {
    teamService.removeTeamMember(clubId, teamId, userId);
    return ResponseEntity.noContent().build();
  }
}
