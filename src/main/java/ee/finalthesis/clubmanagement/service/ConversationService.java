package ee.finalthesis.clubmanagement.service;

import ee.finalthesis.clubmanagement.common.exception.BadRequestException;
import ee.finalthesis.clubmanagement.common.exception.ResourceNotFoundException;
import ee.finalthesis.clubmanagement.domain.Conversation;
import ee.finalthesis.clubmanagement.domain.ConversationParticipant;
import ee.finalthesis.clubmanagement.domain.ConversationReadStatus;
import ee.finalthesis.clubmanagement.domain.Team;
import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.domain.enumeration.ConversationType;
import ee.finalthesis.clubmanagement.repository.ConversationParticipantRepository;
import ee.finalthesis.clubmanagement.repository.ConversationReadStatusRepository;
import ee.finalthesis.clubmanagement.repository.ConversationRepository;
import ee.finalthesis.clubmanagement.repository.UserRepository;
import ee.finalthesis.clubmanagement.security.SecurityUtils;
import ee.finalthesis.clubmanagement.service.dto.chat.ConversationDTO;
import ee.finalthesis.clubmanagement.service.dto.chat.CreateDirectConversationDTO;
import ee.finalthesis.clubmanagement.service.dto.chat.ParticipantDTO;
import ee.finalthesis.clubmanagement.service.mapper.ConversationMapper;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversationService {

  private final ConversationRepository conversationRepository;
  private final ConversationParticipantRepository conversationParticipantRepository;
  private final ConversationReadStatusRepository conversationReadStatusRepository;
  private final UserRepository userRepository;
  private final ConversationMapper conversationMapper;
  private final MessageSource messageSource;

  @Transactional
  public void createTeamConversation(Team team) {
    Conversation conversation =
        Conversation.builder().type(ConversationType.TEAM).team(team).club(team.getClub()).build();
    conversationRepository.save(conversation);
  }

  @Transactional
  public void addParticipantToTeamConversation(UUID teamId, User user) {
    Optional<Conversation> conversationOpt = conversationRepository.findByTeamId(teamId);
    if (conversationOpt.isEmpty()) {
      return;
    }
    Conversation conversation = conversationOpt.get();

    addParticipantIfAbsent(conversation, user);

    // Also add user's parents if they have PARENT role and belong to the same club
    if (user.getParents() != null) {
      for (User parent : user.getParents()) {
        if (parent.getRole() == ClubRole.PARENT
            && parent.getClub() != null
            && parent.getClub().getId().equals(conversation.getClub().getId())) {
          addParticipantIfAbsent(conversation, parent);
        }
      }
    }
  }

  @Transactional
  public void removeParticipantFromTeamConversation(UUID teamId, UUID userId) {
    Optional<Conversation> conversationOpt = conversationRepository.findByTeamId(teamId);
    if (conversationOpt.isEmpty()) {
      return;
    }
    Conversation conversation = conversationOpt.get();

    conversationReadStatusRepository.deleteByConversationIdAndUserId(conversation.getId(), userId);
    conversationParticipantRepository.deleteByConversationIdAndUserId(conversation.getId(), userId);
  }

  @Transactional(readOnly = true)
  public List<ConversationDTO> listConversations(UUID clubId) {
    UUID currentUserId =
        SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new AccessDeniedException(msg("error.auth.notAuthenticated")));

    List<Conversation> conversations =
        conversationParticipantRepository.findConversationsByUserIdAndClubId(currentUserId, clubId);

    return conversations.stream().map(c -> enrichConversationDto(c, currentUserId)).toList();
  }

  @Transactional
  public ConversationDTO createDirectConversation(
      UUID clubId, CreateDirectConversationDTO request) {
    UUID currentUserId =
        SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new AccessDeniedException(msg("error.auth.notAuthenticated")));

    if (currentUserId.equals(request.getParticipantId())) {
      throw new BadRequestException(
          msg("error.conversation.cannotMessageSelf"), "conversation", "cannotMessageSelf");
    }

    User participant =
        userRepository
            .findByIdAndClubId(request.getParticipantId(), clubId)
            .orElseThrow(
                () ->
                    new BadRequestException(
                        msg("error.conversation.participantNotInClub"),
                        "conversation",
                        "participantNotInClub"));

    // Check if direct conversation already exists
    Optional<Conversation> existingConversation =
        conversationParticipantRepository.findDirectConversation(
            currentUserId, request.getParticipantId(), clubId);
    if (existingConversation.isPresent()) {
      return enrichConversationDto(existingConversation.get(), currentUserId);
    }

    User currentUser =
        userRepository
            .findById(currentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUserId));

    Conversation conversation =
        Conversation.builder().type(ConversationType.DIRECT).club(currentUser.getClub()).build();
    conversation = conversationRepository.save(conversation);

    addParticipantWithReadStatus(conversation, currentUser);
    addParticipantWithReadStatus(conversation, participant);

    return enrichConversationDto(conversation, currentUserId);
  }

  @Transactional(readOnly = true)
  public ConversationDTO getConversation(UUID clubId, UUID conversationId) {
    UUID currentUserId =
        SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new AccessDeniedException(msg("error.auth.notAuthenticated")));

    Conversation conversation =
        conversationRepository
            .findByIdAndClubId(conversationId, clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));

    if (!conversationParticipantRepository.existsByConversationIdAndUserId(
        conversationId, currentUserId)) {
      throw new AccessDeniedException(msg("error.conversation.notParticipant"));
    }

    return enrichConversationDto(conversation, currentUserId);
  }

  @Transactional(readOnly = true)
  public int getUnreadCount(UUID clubId) {
    UUID currentUserId =
        SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new AccessDeniedException(msg("error.auth.notAuthenticated")));

    return conversationReadStatusRepository.sumUnreadCountByUserIdAndClubId(currentUserId, clubId);
  }

  private void addParticipantIfAbsent(Conversation conversation, User user) {
    if (!conversationParticipantRepository.existsByConversationIdAndUserId(
        conversation.getId(), user.getId())) {
      addParticipantWithReadStatus(conversation, user);
    }
  }

  private void addParticipantWithReadStatus(Conversation conversation, User user) {
    ConversationParticipant participant =
        ConversationParticipant.builder().conversation(conversation).user(user).build();
    conversationParticipantRepository.save(participant);

    ConversationReadStatus readStatus =
        ConversationReadStatus.builder()
            .conversation(conversation)
            .user(user)
            .unreadCount(0)
            .build();
    conversationReadStatusRepository.save(readStatus);
  }

  private ConversationDTO enrichConversationDto(Conversation conversation, UUID currentUserId) {
    ConversationDTO dto = conversationMapper.toDto(conversation);

    // Set participants
    List<ConversationParticipant> participants =
        conversationParticipantRepository.findByConversationId(conversation.getId());
    List<ParticipantDTO> participantDtos = conversationMapper.toParticipantDto(participants);
    dto.setParticipants(participantDtos);

    // Set name
    if (conversation.getType() == ConversationType.TEAM && conversation.getTeam() != null) {
      dto.setName(conversation.getTeam().getName());
    } else if (conversation.getType() == ConversationType.DIRECT) {
      participantDtos.stream()
          .filter(p -> !p.getUserId().equals(currentUserId))
          .findFirst()
          .ifPresent(p -> dto.setName(p.getFirstName() + " " + p.getLastName()));
    }

    // Set last message sender name
    if (conversation.getLastMessageSender() != null) {
      dto.setLastMessageSenderName(
          conversation.getLastMessageSender().getFirstName()
              + " "
              + conversation.getLastMessageSender().getLastName());
    }

    // Set unread count
    conversationReadStatusRepository
        .findByConversationIdAndUserId(conversation.getId(), currentUserId)
        .ifPresent(rs -> dto.setUnreadCount(rs.getUnreadCount()));

    return dto;
  }

  private String msg(String key, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(key, args, key, locale);
  }
}
