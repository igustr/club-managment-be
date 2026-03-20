package ee.finalthesis.clubmanagement.service;

import ee.finalthesis.clubmanagement.domain.Club;
import ee.finalthesis.clubmanagement.domain.Notification;
import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.domain.enumeration.NotificationType;
import ee.finalthesis.clubmanagement.repository.NotificationRepository;
import ee.finalthesis.clubmanagement.security.SecurityUtils;
import ee.finalthesis.clubmanagement.service.dto.notification.NotificationDTO;
import ee.finalthesis.clubmanagement.service.mapper.NotificationMapper;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final NotificationMapper notificationMapper;

  @Transactional(readOnly = true)
  public Page<NotificationDTO> getNotifications(UUID clubId, Pageable pageable) {
    UUID userId =
        SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("Not authenticated"));
    return notificationRepository
        .findByUserIdAndClubIdOrderByCreatedAtDesc(userId, clubId, pageable)
        .map(notificationMapper::toDto);
  }

  @Transactional(readOnly = true)
  public long getUnreadCount(UUID clubId) {
    UUID userId =
        SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("Not authenticated"));
    return notificationRepository.countByUserIdAndClubIdAndReadFalse(userId, clubId);
  }

  @Transactional
  public void markAsRead(UUID clubId, UUID notificationId) {
    notificationRepository
        .findById(notificationId)
        .ifPresent(
            n -> {
              n.setRead(true);
              notificationRepository.save(n);
            });
  }

  @Transactional
  public void markAllAsRead(UUID clubId) {
    UUID userId =
        SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("Not authenticated"));
    notificationRepository.markAllAsRead(userId, clubId);
  }

  /**
   * Creates a notification for each recipient. The current user (actor) is excluded from
   * recipients.
   */
  @Transactional
  public void notifyUsers(
      Collection<User> recipients,
      Club club,
      NotificationType type,
      String title,
      String message,
      UUID referenceId) {
    UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    Instant now = Instant.now();

    for (User recipient : recipients) {
      if (recipient.getId().equals(currentUserId)) {
        continue;
      }
      Notification notification =
          Notification.builder()
              .user(recipient)
              .club(club)
              .type(type)
              .title(title)
              .message(message)
              .referenceId(referenceId)
              .read(false)
              .createdAt(now)
              .build();
      notificationRepository.save(notification);
    }
  }
}
