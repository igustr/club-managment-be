package ee.finalthesis.clubmanagement.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import ee.finalthesis.clubmanagement.common.exception.BadRequestException;
import ee.finalthesis.clubmanagement.common.exception.ConflictException;
import ee.finalthesis.clubmanagement.common.exception.ResourceNotFoundException;
import ee.finalthesis.clubmanagement.domain.Club;
import ee.finalthesis.clubmanagement.domain.Team;
import ee.finalthesis.clubmanagement.domain.TeamMember;
import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.repository.ClubRepository;
import ee.finalthesis.clubmanagement.repository.TeamMemberRepository;
import ee.finalthesis.clubmanagement.repository.UserRepository;
import ee.finalthesis.clubmanagement.service.dto.auth.UserDTO;
import ee.finalthesis.clubmanagement.service.dto.user.LinkParentDTO;
import ee.finalthesis.clubmanagement.service.mapper.UserMapper;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private ClubRepository clubRepository;
  @Mock private TeamMemberRepository teamMemberRepository;
  @Mock private ConversationService conversationService;
  @Mock private UserMapper userMapper;
  @Mock private MessageSource messageSource;

  @InjectMocks private UserService userService;

  @Test
  void linkParent_shouldAddAndSyncConversations() {
    UUID clubId = UUID.randomUUID();
    UUID childId = UUID.randomUUID();
    UUID parentId = UUID.randomUUID();
    UUID teamId = UUID.randomUUID();

    Club club = Club.builder().id(clubId).build();
    User child =
        User.builder()
            .id(childId)
            .club(club)
            .role(ClubRole.PLAYER)
            .parents(new HashSet<>())
            .build();
    User parent = User.builder().id(parentId).club(club).role(ClubRole.PARENT).build();
    Team team = Team.builder().id(teamId).club(club).build();
    TeamMember tm = TeamMember.builder().team(team).user(child).build();

    LinkParentDTO dto = new LinkParentDTO();
    dto.setParentId(parentId);

    when(userRepository.findByIdAndClubId(childId, clubId)).thenReturn(Optional.of(child));
    when(userRepository.findByIdAndClubId(parentId, clubId)).thenReturn(Optional.of(parent));
    when(userRepository.existsParentChildRelationship(childId, parentId)).thenReturn(false);
    when(userRepository.save(child)).thenReturn(child);
    when(teamMemberRepository.findByUserId(childId)).thenReturn(List.of(tm));
    when(userMapper.toDto(any(User.class))).thenReturn(new UserDTO());

    userService.linkParent(clubId, childId, dto);

    verify(userRepository).save(child);
    verify(conversationService).addParticipantToTeamConversation(teamId, parent);
  }

  @Test
  void linkParent_selfLink_shouldThrow() {
    UUID clubId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    Club club = Club.builder().id(clubId).build();
    User user = User.builder().id(userId).club(club).build();

    LinkParentDTO dto = new LinkParentDTO();
    dto.setParentId(userId);

    when(userRepository.findByIdAndClubId(userId, clubId)).thenReturn(Optional.of(user));
    when(messageSource.getMessage(any(), any(), any(), any())).thenReturn("Cannot link self");

    assertThatThrownBy(() -> userService.linkParent(clubId, userId, dto))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void linkParent_parentNotInClub_shouldThrow() {
    UUID clubId = UUID.randomUUID();
    UUID childId = UUID.randomUUID();
    UUID parentId = UUID.randomUUID();

    Club club = Club.builder().id(clubId).build();
    User child = User.builder().id(childId).club(club).build();

    LinkParentDTO dto = new LinkParentDTO();
    dto.setParentId(parentId);

    when(userRepository.findByIdAndClubId(childId, clubId)).thenReturn(Optional.of(child));
    when(userRepository.findByIdAndClubId(parentId, clubId)).thenReturn(Optional.empty());
    when(messageSource.getMessage(any(), any(), any(), any())).thenReturn("Parent not in club");

    assertThatThrownBy(() -> userService.linkParent(clubId, childId, dto))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void linkParent_wrongRole_shouldThrow() {
    UUID clubId = UUID.randomUUID();
    UUID childId = UUID.randomUUID();
    UUID parentId = UUID.randomUUID();

    Club club = Club.builder().id(clubId).build();
    User child = User.builder().id(childId).club(club).build();
    User parent = User.builder().id(parentId).club(club).role(ClubRole.PLAYER).build();

    LinkParentDTO dto = new LinkParentDTO();
    dto.setParentId(parentId);

    when(userRepository.findByIdAndClubId(childId, clubId)).thenReturn(Optional.of(child));
    when(userRepository.findByIdAndClubId(parentId, clubId)).thenReturn(Optional.of(parent));
    when(messageSource.getMessage(any(), any(), any(), any())).thenReturn("Parent role required");

    assertThatThrownBy(() -> userService.linkParent(clubId, childId, dto))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  void linkParent_alreadyLinked_shouldThrow() {
    UUID clubId = UUID.randomUUID();
    UUID childId = UUID.randomUUID();
    UUID parentId = UUID.randomUUID();

    Club club = Club.builder().id(clubId).build();
    User child = User.builder().id(childId).club(club).build();
    User parent = User.builder().id(parentId).club(club).role(ClubRole.PARENT).build();

    LinkParentDTO dto = new LinkParentDTO();
    dto.setParentId(parentId);

    when(userRepository.findByIdAndClubId(childId, clubId)).thenReturn(Optional.of(child));
    when(userRepository.findByIdAndClubId(parentId, clubId)).thenReturn(Optional.of(parent));
    when(userRepository.existsParentChildRelationship(childId, parentId)).thenReturn(true);
    when(messageSource.getMessage(any(), any(), any(), any())).thenReturn("Already linked");

    assertThatThrownBy(() -> userService.linkParent(clubId, childId, dto))
        .isInstanceOf(ConflictException.class);
  }

  @Test
  void unlinkParent_shouldRemoveAndSyncConversations() {
    UUID clubId = UUID.randomUUID();
    UUID childId = UUID.randomUUID();
    UUID parentId = UUID.randomUUID();
    UUID teamId = UUID.randomUUID();

    Club club = Club.builder().id(clubId).build();
    User parent = User.builder().id(parentId).club(club).role(ClubRole.PARENT).build();
    User child =
        User.builder()
            .id(childId)
            .club(club)
            .role(ClubRole.PLAYER)
            .parents(new HashSet<>(Set.of(parent)))
            .build();
    Team team = Team.builder().id(teamId).club(club).build();
    TeamMember tm = TeamMember.builder().team(team).user(child).build();

    when(userRepository.findByIdAndClubId(childId, clubId)).thenReturn(Optional.of(child));
    when(userRepository.existsParentChildRelationship(childId, parentId)).thenReturn(true);
    when(userRepository.findById(parentId)).thenReturn(Optional.of(parent));
    when(teamMemberRepository.findByUserId(childId)).thenReturn(List.of(tm));
    when(userRepository.findChildrenByParentId(parentId)).thenReturn(List.of(child));

    userService.unlinkParent(clubId, childId, parentId);

    verify(userRepository).save(child);
    verify(conversationService).removeParticipantFromTeamConversation(teamId, parentId);
  }

  @Test
  void unlinkParent_parentHasOtherChild_shouldKeepInConversation() {
    UUID clubId = UUID.randomUUID();
    UUID childId = UUID.randomUUID();
    UUID otherChildId = UUID.randomUUID();
    UUID parentId = UUID.randomUUID();
    UUID teamId = UUID.randomUUID();

    Club club = Club.builder().id(clubId).build();
    User parent = User.builder().id(parentId).club(club).role(ClubRole.PARENT).build();
    User child =
        User.builder()
            .id(childId)
            .club(club)
            .role(ClubRole.PLAYER)
            .parents(new HashSet<>(Set.of(parent)))
            .build();
    User otherChild = User.builder().id(otherChildId).club(club).role(ClubRole.PLAYER).build();
    Team team = Team.builder().id(teamId).club(club).build();
    TeamMember tm = TeamMember.builder().team(team).user(child).build();

    when(userRepository.findByIdAndClubId(childId, clubId)).thenReturn(Optional.of(child));
    when(userRepository.existsParentChildRelationship(childId, parentId)).thenReturn(true);
    when(userRepository.findById(parentId)).thenReturn(Optional.of(parent));
    when(teamMemberRepository.findByUserId(childId)).thenReturn(List.of(tm));
    when(userRepository.findChildrenByParentId(parentId)).thenReturn(List.of(child, otherChild));
    when(teamMemberRepository.findTeamIdsByUserIdsAndTeamIds(
            List.of(otherChildId), List.of(teamId)))
        .thenReturn(List.of(teamId));

    userService.unlinkParent(clubId, childId, parentId);

    verify(conversationService, never()).removeParticipantFromTeamConversation(any(), any());
  }

  @Test
  void unlinkParent_notLinked_shouldThrow() {
    UUID clubId = UUID.randomUUID();
    UUID childId = UUID.randomUUID();
    UUID parentId = UUID.randomUUID();

    Club club = Club.builder().id(clubId).build();
    User child = User.builder().id(childId).club(club).build();

    when(userRepository.findByIdAndClubId(childId, clubId)).thenReturn(Optional.of(child));
    when(userRepository.existsParentChildRelationship(childId, parentId)).thenReturn(false);
    when(messageSource.getMessage(any(), any(), any(), any())).thenReturn("Not linked");

    assertThatThrownBy(() -> userService.unlinkParent(clubId, childId, parentId))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
