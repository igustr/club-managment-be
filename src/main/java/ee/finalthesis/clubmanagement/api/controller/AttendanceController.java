package ee.finalthesis.clubmanagement.api.controller;

import ee.finalthesis.clubmanagement.service.AttendanceService;
import ee.finalthesis.clubmanagement.service.dto.attendance.AttendanceDTO;
import ee.finalthesis.clubmanagement.service.dto.attendance.AttendanceSummaryDTO;
import ee.finalthesis.clubmanagement.service.dto.attendance.UpdateAttendanceDTO;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/clubs/{clubId}/trainings/{trainingId}/attendance")
@RequiredArgsConstructor
public class AttendanceController {

  private final AttendanceService attendanceService;

  @GetMapping
  @PreAuthorize("@clubSecurity.isAdminOrCoach(#clubId)")
  public ResponseEntity<List<AttendanceDTO>> getAttendanceList(
      @PathVariable UUID clubId, @PathVariable UUID trainingId) {
    return ResponseEntity.ok(attendanceService.getAttendanceList(clubId, trainingId));
  }

  @GetMapping("/summary")
  @PreAuthorize("@clubSecurity.isAdminOrCoach(#clubId)")
  public ResponseEntity<AttendanceSummaryDTO> getAttendanceSummary(
      @PathVariable UUID clubId, @PathVariable UUID trainingId) {
    return ResponseEntity.ok(attendanceService.getAttendanceSummary(clubId, trainingId));
  }

  @PutMapping("/{userId}")
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<AttendanceDTO> updateAttendance(
      @PathVariable UUID clubId,
      @PathVariable UUID trainingId,
      @PathVariable UUID userId,
      @Valid @RequestBody UpdateAttendanceDTO request) {
    return ResponseEntity.ok(
        attendanceService.updateAttendance(clubId, trainingId, userId, request));
  }
}
