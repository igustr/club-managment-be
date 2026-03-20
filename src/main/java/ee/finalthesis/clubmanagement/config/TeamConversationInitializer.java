package ee.finalthesis.clubmanagement.config;

import ee.finalthesis.clubmanagement.domain.Team;
import ee.finalthesis.clubmanagement.domain.TeamMember;
import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.repository.ConversationRepository;
import ee.finalthesis.clubmanagement.repository.TeamMemberRepository;
import ee.finalthesis.clubmanagement.repository.TeamRepository;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TeamConversationInitializer {

  private static final String SYSTEM_USER = "system";

  private final TeamRepository teamRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final ConversationRepository conversationRepository;
  private final JdbcTemplate jdbcTemplate;

  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void initializeMissingTeamConversations() {
    List<UUID> teamIds = conversationRepository.findTeamIdsWithoutConversation();

    if (teamIds.isEmpty()) {
      return;
    }

    List<Team> teams = teamRepository.findAllById(teamIds);
    Instant now = Instant.now();
    Timestamp ts = Timestamp.from(now);

    for (Team team : teams) {
      UUID conversationId = UUID.randomUUID();

      // Insert conversation via JDBC
      jdbcTemplate.update(
          "INSERT INTO conversation (id, type, name, team_id, club_id, created_by, created_date,"
              + " last_modified_by, last_modified_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
          conversationId,
          "TEAM",
          null,
          team.getId(),
          team.getClub().getId(),
          SYSTEM_USER,
          ts,
          SYSTEM_USER,
          ts);

      // Collect participants
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

      List<User> participantList = new ArrayList<>(participants);

      // Batch insert participants
      jdbcTemplate.batchUpdate(
          "INSERT INTO conversation_participant (id, conversation_id, user_id, created_by,"
              + " created_date, last_modified_by, last_modified_date) VALUES (?, ?, ?, ?, ?, ?, ?)",
          participantList,
          participantList.size(),
          (PreparedStatement ps, User user) -> {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, conversationId);
            ps.setObject(3, user.getId());
            ps.setString(4, SYSTEM_USER);
            ps.setTimestamp(5, ts);
            ps.setString(6, SYSTEM_USER);
            ps.setTimestamp(7, ts);
          });

      // Batch insert read statuses
      jdbcTemplate.batchUpdate(
          "INSERT INTO conversation_read_status (id, conversation_id, user_id, unread_count,"
              + " last_read_at, created_by, created_date, last_modified_by, last_modified_date)"
              + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
          participantList,
          participantList.size(),
          (PreparedStatement ps, User user) -> {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, conversationId);
            ps.setObject(3, user.getId());
            ps.setInt(4, 0);
            ps.setTimestamp(5, null);
            ps.setString(6, SYSTEM_USER);
            ps.setTimestamp(7, ts);
            ps.setString(8, SYSTEM_USER);
            ps.setTimestamp(9, ts);
          });
    }

    log.info("Created team conversations for {} teams", teamIds.size());
  }
}
