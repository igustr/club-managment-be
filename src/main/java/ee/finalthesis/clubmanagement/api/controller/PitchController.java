package ee.finalthesis.clubmanagement.api.controller;

import ee.finalthesis.clubmanagement.service.PitchService;
import ee.finalthesis.clubmanagement.service.dto.pitch.CreatePitchDTO;
import ee.finalthesis.clubmanagement.service.dto.pitch.PitchDTO;
import ee.finalthesis.clubmanagement.service.dto.pitch.UpdatePitchDTO;
import ee.finalthesis.clubmanagement.service.dto.training.TrainingSessionDTO;
import jakarta.validation.Valid;
import java.time.LocalDate;
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
@RequestMapping("/api/clubs/{clubId}/pitches")
@RequiredArgsConstructor
public class PitchController {

  private final PitchService pitchService;

  @GetMapping
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<List<PitchDTO>> listPitches(@PathVariable UUID clubId) {
    return ResponseEntity.ok(pitchService.listPitches(clubId));
  }

  @PostMapping
  @PreAuthorize("@clubSecurity.isAdmin(#clubId)")
  public ResponseEntity<PitchDTO> createPitch(
      @PathVariable UUID clubId, @Valid @RequestBody CreatePitchDTO request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(pitchService.createPitch(clubId, request));
  }

  @GetMapping("/{pitchId}")
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<PitchDTO> getPitch(@PathVariable UUID clubId, @PathVariable UUID pitchId) {
    return ResponseEntity.ok(pitchService.getPitch(clubId, pitchId));
  }

  @PutMapping("/{pitchId}")
  @PreAuthorize("@clubSecurity.isAdmin(#clubId)")
  public ResponseEntity<PitchDTO> updatePitch(
      @PathVariable UUID clubId,
      @PathVariable UUID pitchId,
      @Valid @RequestBody UpdatePitchDTO request) {
    return ResponseEntity.ok(pitchService.updatePitch(clubId, pitchId, request));
  }

  @DeleteMapping("/{pitchId}")
  @PreAuthorize("@clubSecurity.isAdmin(#clubId)")
  public ResponseEntity<Void> deletePitch(@PathVariable UUID clubId, @PathVariable UUID pitchId) {
    pitchService.deletePitch(clubId, pitchId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{pitchId}/schedule")
  @PreAuthorize("@clubSecurity.isAdmin(#clubId)")
  public ResponseEntity<List<TrainingSessionDTO>> getPitchSchedule(
      @PathVariable UUID clubId,
      @PathVariable UUID pitchId,
      @RequestParam LocalDate startDate,
      @RequestParam LocalDate endDate) {
    return ResponseEntity.ok(pitchService.getPitchSchedule(clubId, pitchId, startDate, endDate));
  }
}
