package ee.finalthesis.clubmanagement.api.controller;

import ee.finalthesis.clubmanagement.service.StatisticsService;
import ee.finalthesis.clubmanagement.service.dto.statistics.ClubStatisticsDTO;
import ee.finalthesis.clubmanagement.service.dto.statistics.PlayerStatisticsDTO;
import ee.finalthesis.clubmanagement.service.dto.statistics.TeamStatisticsDTO;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clubs/{clubId}")
@RequiredArgsConstructor
public class StatisticsController {

  private final StatisticsService statisticsService;

  @GetMapping("/users/{userId}/statistics")
  @PreAuthorize("@clubSecurity.isAdminOrCoach(#clubId)")
  public ResponseEntity<PlayerStatisticsDTO> getPlayerStatistics(
      @PathVariable UUID clubId, @PathVariable UUID userId) {
    return ResponseEntity.ok(statisticsService.getPlayerStatistics(clubId, userId));
  }

  @GetMapping("/teams/{teamId}/statistics")
  @PreAuthorize("@clubSecurity.isAdminOrCoach(#clubId)")
  public ResponseEntity<TeamStatisticsDTO> getTeamStatistics(
      @PathVariable UUID clubId, @PathVariable UUID teamId) {
    return ResponseEntity.ok(statisticsService.getTeamStatistics(clubId, teamId));
  }

  @GetMapping("/statistics")
  @PreAuthorize("@clubSecurity.isAdminOrCoach(#clubId)")
  public ResponseEntity<ClubStatisticsDTO> getClubStatistics(@PathVariable UUID clubId) {
    return ResponseEntity.ok(statisticsService.getClubStatistics(clubId));
  }
}
