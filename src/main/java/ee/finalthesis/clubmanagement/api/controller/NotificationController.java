package ee.finalthesis.clubmanagement.api.controller;

import ee.finalthesis.clubmanagement.service.NotificationService;
import ee.finalthesis.clubmanagement.service.dto.notification.NotificationDTO;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clubs/{clubId}/notifications")
@RequiredArgsConstructor
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<Page<NotificationDTO>> getNotifications(
      @PathVariable UUID clubId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ResponseEntity.ok(
        notificationService.getNotifications(
            clubId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))));
  }

  @GetMapping("/unread-count")
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<Long> getUnreadCount(@PathVariable UUID clubId) {
    return ResponseEntity.ok(notificationService.getUnreadCount(clubId));
  }

  @PutMapping("/{notificationId}/read")
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<Void> markAsRead(
      @PathVariable UUID clubId, @PathVariable UUID notificationId) {
    notificationService.markAsRead(clubId, notificationId);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/read-all")
  @PreAuthorize("@clubSecurity.isMemberOfClub(#clubId)")
  public ResponseEntity<Void> markAllAsRead(@PathVariable UUID clubId) {
    notificationService.markAllAsRead(clubId);
    return ResponseEntity.noContent().build();
  }
}
