package ee.finalthesis.clubmanagement.service;

import ee.finalthesis.clubmanagement.common.exception.ResourceNotFoundException;
import ee.finalthesis.clubmanagement.domain.Conversation;
import ee.finalthesis.clubmanagement.domain.ConversationReadStatus;
import ee.finalthesis.clubmanagement.domain.Message;
import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.repository.ConversationParticipantRepository;
import ee.finalthesis.clubmanagement.repository.ConversationReadStatusRepository;
import ee.finalthesis.clubmanagement.repository.ConversationRepository;
import ee.finalthesis.clubmanagement.repository.MessageRepository;
import ee.finalthesis.clubmanagement.repository.UserRepository;
import ee.finalthesis.clubmanagement.security.SecurityUtils;
import ee.finalthesis.clubmanagement.service.dto.chat.MessageDTO;
import ee.finalthesis.clubmanagement.service.dto.chat.SendMessageDTO;
import ee.finalthesis.clubmanagement.service.mapper.MessageMapper;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageService {

  private final MessageRepository messageRepository;
  private final ConversationRepository conversationRepository;
  private final ConversationParticipantRepository conversationParticipantRepository;
  private final ConversationReadStatusRepository conversationReadStatusRepository;
  private final UserRepository userRepository;
  private final MessageMapper messageMapper;
  private final MessageSource messageSource;

  @Transactional(readOnly = true)
  public Page<MessageDTO> getMessages(UUID clubId, UUID conversationId, Pageable pageable) {
    UUID currentUserId =
        SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new AccessDeniedException(msg("error.auth.notAuthenticated")));

    Conversation conversation = findConversationInClub(clubId, conversationId);

    if (!conversationParticipantRepository.existsByConversationIdAndUserId(
        conversation.getId(), currentUserId)) {
      throw new AccessDeniedException(msg("error.conversation.notParticipant"));
    }

    return messageRepository
        .findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
        .map(messageMapper::toDto);
  }

  @Transactional
  public MessageDTO sendMessage(UUID clubId, UUID conversationId, SendMessageDTO request) {
    UUID currentUserId =
        SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new AccessDeniedException(msg("error.auth.notAuthenticated")));

    Conversation conversation = findConversationInClub(clubId, conversationId);

    if (!conversationParticipantRepository.existsByConversationIdAndUserId(
        conversationId, currentUserId)) {
      throw new AccessDeniedException(msg("error.conversation.notParticipant"));
    }

    User sender =
        userRepository
            .findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

    Instant now = Instant.now();

    Message message =
        Message.builder()
            .text(request.getText())
            .createdAt(now)
            .sender(sender)
            .conversation(conversation)
            .build();
    message = messageRepository.save(message);

    // Update conversation with last message info
    conversation.setLastMessageText(request.getText());
    conversation.setLastMessageTime(now);
    conversation.setLastMessageSender(sender);
    conversationRepository.save(conversation);

    // Increment unread count for all participants except sender
    conversationReadStatusRepository.incrementUnreadCountExcludingUser(
        conversationId, currentUserId);

    return messageMapper.toDto(message);
  }

  @Transactional
  public void markAsRead(UUID clubId, UUID conversationId) {
    UUID currentUserId =
        SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new AccessDeniedException(msg("error.auth.notAuthenticated")));

    findConversationInClub(clubId, conversationId);

    if (!conversationParticipantRepository.existsByConversationIdAndUserId(
        conversationId, currentUserId)) {
      throw new AccessDeniedException(msg("error.conversation.notParticipant"));
    }

    ConversationReadStatus readStatus =
        conversationReadStatusRepository
            .findByConversationIdAndUserId(conversationId, currentUserId)
            .orElseGet(
                () -> {
                  Conversation conversation =
                      conversationRepository.getReferenceById(conversationId);
                  User user = userRepository.getReferenceById(currentUserId);
                  return ConversationReadStatus.builder()
                      .conversation(conversation)
                      .user(user)
                      .unreadCount(0)
                      .build();
                });

    readStatus.setUnreadCount(0);
    readStatus.setLastReadAt(Instant.now());
    conversationReadStatusRepository.save(readStatus);
  }

  private Conversation findConversationInClub(UUID clubId, UUID conversationId) {
    return conversationRepository
        .findByIdAndClubId(conversationId, clubId)
        .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));
  }

  private String msg(String key, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(key, args, key, locale);
  }
}
