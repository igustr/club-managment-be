package ee.finalthesis.clubmanagement.config;

import ee.finalthesis.clubmanagement.domain.Conversation;
import ee.finalthesis.clubmanagement.domain.ConversationParticipant;
import ee.finalthesis.clubmanagement.domain.ConversationReadStatus;
import ee.finalthesis.clubmanagement.domain.Team;
import ee.finalthesis.clubmanagement.domain.TeamMember;
import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.domain.enumeration.ConversationType;
import ee.finalthesis.clubmanagement.repository.ConversationParticipantRepository;
import ee.finalthesis.clubmanagement.repository.ConversationReadStatusRepository;
import ee.finalthesis.clubmanagement.repository.ConversationRepository;
import ee.finalthesis.clubmanagement.repository.TeamMemberRepository;
import ee.finalthesis.clubmanagement.repository.TeamRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TeamConversationInitializer {

  private final TeamRepository teamRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final ConversationRepository conversationRepository;
  private final ConversationParticipantRepository conversationParticipantRepository;
  private final ConversationReadStatusRepository conversationReadStatusRepository;

  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void initializeMissingTeamConversations() {
    List<UUID> teamIds = conversationRepository.findTeamIdsWithoutConversation();

    if (teamIds.isEmpty()) {
      return;
    }

    List<Team> teams = teamRepository.findAllById(teamIds);

    for (Team team : teams) {
      Conversation conversation =
          Conversation.builder()
              .type(ConversationType.TEAM)
              .team(team)
              .club(team.getClub())
              .build();
      conversation = conversationRepository.save(conversation);

      // Single query: fetch members with users and parents eagerly loaded
      List<TeamMember> members = teamMemberRepository.findByTeamIdWithUsersAndParents(team.getId());
      Set<User> participants = new LinkedHashSet<>();
      for (TeamMember member : members) {
        participants.add(member.getUser());
        for (User parent : member.getUser().getParents()) {
          if (parent.getRole() == ClubRole.PARENT
              && parent.getClub() != null
              && parent.getClub().getId().equals(team.getClub().getId())) {
            participants.add(parent);
          }
        }
      }

      // Batch insert all participants and read statuses
      List<ConversationParticipant> participantEntities = new ArrayList<>();
      List<ConversationReadStatus> readStatuses = new ArrayList<>();
      for (User user : participants) {
        participantEntities.add(
            ConversationParticipant.builder().conversation(conversation).user(user).build());
        readStatuses.add(
            ConversationReadStatus.builder()
                .conversation(conversation)
                .user(user)
                .unreadCount(0)
                .build());
      }
      conversationParticipantRepository.saveAll(participantEntities);
      conversationReadStatusRepository.saveAll(readStatuses);
    }

    log.info("Created team conversations for {} teams", teamIds.size());
  }
}
