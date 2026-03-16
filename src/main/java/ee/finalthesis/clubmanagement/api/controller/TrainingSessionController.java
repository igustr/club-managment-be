package ee.finalthesis.clubmanagement.api.controller;

import ee.finalthesis.clubmanagement.service.TrainingSessionService;
import ee.finalthesis.clubmanagement.service.dto.training.CreateRecurringTrainingDTO;
import ee.finalthesis.clubmanagement.service.dto.training.CreateTrainingSessionDTO;
import ee.finalthesis.clubmanagement.service.dto.training.TrainingSessionDTO;
import ee.finalthesis.clubmanagement.service.dto.training.UpdateTrainingSessionDTO;
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
public class TrainingSessionController {

  private final TrainingSessionService trainingSessionService;

  @GetMapping("/trainings")
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<List<TrainingSessionDTO>> listTrainings(
      @PathVariable UUID clubId, @RequestParam(defaultValue = "false") boolean myTeams) {
    return ResponseEntity.ok(trainingSessionService.listTrainingsByClub(clubId, myTeams));
  }

  @GetMapping("/trainings/{trainingId}")
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<TrainingSessionDTO> getTraining(
      @PathVariable UUID clubId, @PathVariable UUID trainingId) {
    return ResponseEntity.ok(trainingSessionService.getTraining(clubId, trainingId));
  }

  @PostMapping("/teams/{teamId}/trainings")
  @PreAuthorize("@teamSecurity.canManageTeam(#teamId)")
  public ResponseEntity<TrainingSessionDTO> createTraining(
      @PathVariable UUID clubId,
      @PathVariable UUID teamId,
      @Valid @RequestBody CreateTrainingSessionDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(trainingSessionService.createTraining(clubId, teamId, request));
  }

  @PostMapping("/teams/{teamId}/trainings/recurring")
  @PreAuthorize("@teamSecurity.canManageTeam(#teamId)")
  public ResponseEntity<List<TrainingSessionDTO>> createRecurringTraining(
      @PathVariable UUID clubId,
      @PathVariable UUID teamId,
      @Valid @RequestBody CreateRecurringTrainingDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(trainingSessionService.createRecurringTraining(clubId, teamId, request));
  }

  @PutMapping("/trainings/{trainingId}")
  @PreAuthorize("@clubSecurity.isAdminOrCoach(#clubId)")
  public ResponseEntity<TrainingSessionDTO> updateTraining(
      @PathVariable UUID clubId,
      @PathVariable UUID trainingId,
      @Valid @RequestBody UpdateTrainingSessionDTO request) {
    return ResponseEntity.ok(trainingSessionService.updateTraining(clubId, trainingId, request));
  }

  @PutMapping("/trainings/{trainingId}/cancel")
  @PreAuthorize("@clubSecurity.isAdminOrCoach(#clubId)")
  public ResponseEntity<Void> cancelTraining(
      @PathVariable UUID clubId, @PathVariable UUID trainingId) {
    trainingSessionService.cancelTraining(clubId, trainingId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/trainings/{trainingId}")
  @PreAuthorize("@clubSecurity.isAdmin(#clubId)")
  public ResponseEntity<Void> deleteTraining(
      @PathVariable UUID clubId, @PathVariable UUID trainingId) {
    trainingSessionService.deleteTraining(clubId, trainingId);
    return ResponseEntity.noContent().build();
  }
}
