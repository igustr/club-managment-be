package ee.finalthesis.clubmanagement.service;

import ee.finalthesis.clubmanagement.common.exception.BadRequestException;
import ee.finalthesis.clubmanagement.common.exception.ConflictException;
import ee.finalthesis.clubmanagement.common.exception.ResourceNotFoundException;
import ee.finalthesis.clubmanagement.domain.Club;
import ee.finalthesis.clubmanagement.domain.TeamMember;
import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.repository.ClubRepository;
import ee.finalthesis.clubmanagement.repository.TeamMemberRepository;
import ee.finalthesis.clubmanagement.repository.UserRepository;
import ee.finalthesis.clubmanagement.security.SecurityUtils;
import ee.finalthesis.clubmanagement.service.dto.auth.UserDTO;
import ee.finalthesis.clubmanagement.service.dto.user.AddUserToClubDTO;
import ee.finalthesis.clubmanagement.service.dto.user.LinkParentDTO;
import ee.finalthesis.clubmanagement.service.dto.user.UpdateUserDTO;
import ee.finalthesis.clubmanagement.service.mapper.UserMapper;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final ClubRepository clubRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final ConversationService conversationService;
  private final UserMapper userMapper;
  private final MessageSource messageSource;

  @Transactional(readOnly = true)
  public Page<UserDTO> listClubUsers(UUID clubId, Pageable pageable) {
    return userRepository.findByClubId(clubId, pageable).map(userMapper::toDto);
  }

  @Transactional(readOnly = true)
  public UserDTO getClubUser(UUID clubId, UUID userId) {
    User user =
        userRepository
            .findByIdAndClubId(userId, clubId)
            .orElseThrow(
                () -> new ResourceNotFoundException(msg("error.user.notInClub"), "user", userId));
    return userMapper.toDto(user);
  }

  @Transactional
  public UserDTO addUserToClub(UUID clubId, AddUserToClubDTO request) {
    Club club =
        clubRepository
            .findById(clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Club", "id", clubId));

    User user =
        userRepository
            .findById(request.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getUserId()));

    if (user.getClub() != null) {
      throw new ConflictException(msg("error.user.alreadyInClub"), "user", "alreadyInClub");
    }

    user.setClub(club);
    user.setRole(request.getRole());
    user = userRepository.save(user);
    return userMapper.toDto(user);
  }

  @Transactional
  public UserDTO updateUser(UUID clubId, UUID userId, UpdateUserDTO request) {
    User user =
        userRepository
            .findByIdAndClubId(userId, clubId)
            .orElseThrow(
                () -> new ResourceNotFoundException(msg("error.user.notInClub"), "user", userId));

    if (request.getFirstName() != null) {
      user.setFirstName(request.getFirstName());
    }
    if (request.getLastName() != null) {
      user.setLastName(request.getLastName());
    }
    if (request.getPhone() != null) {
      user.setPhone(request.getPhone());
    }
    if (request.getPhotoUrl() != null) {
      user.setPhotoUrl(request.getPhotoUrl());
    }
    if (request.getRole() != null) {
      user.setRole(request.getRole());
    }
    if (request.getActive() != null) {
      user.setActive(request.getActive());
    }

    user = userRepository.save(user);
    return userMapper.toDto(user);
  }

  @Transactional
  public void removeUserFromClub(UUID clubId, UUID userId) {
    UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
    if (userId.equals(currentUserId)) {
      throw new BadRequestException(msg("error.user.cannotRemoveSelf"), "user", "cannotRemoveSelf");
    }

    User user =
        userRepository
            .findByIdAndClubId(userId, clubId)
            .orElseThrow(
                () -> new ResourceNotFoundException(msg("error.user.notInClub"), "user", userId));

    teamMemberRepository.deleteByUserId(userId);

    user.setClub(null);
    user.setRole(null);
    userRepository.save(user);
  }

  @Transactional(readOnly = true)
  public Page<UserDTO> listUnaffiliatedUsers(String search, Pageable pageable) {
    if (search != null && !search.isBlank()) {
      return userRepository
          .findUnaffiliatedBySearch(search.trim(), pageable)
          .map(userMapper::toDto);
    }
    return userRepository.findByClubIdIsNull(pageable).map(userMapper::toDto);
  }

  @Transactional(readOnly = true)
  public List<UserDTO> listParents(UUID clubId, UUID userId) {
    validateUserInClub(clubId, userId);
    return userRepository.findParentsByChildId(userId).stream().map(userMapper::toDto).toList();
  }

  @Transactional
  public UserDTO linkParent(UUID clubId, UUID userId, LinkParentDTO dto) {
    User child = validateUserInClub(clubId, userId);

    if (userId.equals(dto.getParentId())) {
      throw new BadRequestException(
          msg("error.parentChild.cannotLinkSelf"), "parentChild", "cannotLinkSelf");
    }

    User parent =
        userRepository
            .findByIdAndClubId(dto.getParentId(), clubId)
            .orElseThrow(
                () ->
                    new BadRequestException(
                        msg("error.parentChild.parentNotInClub"),
                        "parentChild",
                        "parentNotInClub"));

    if (parent.getRole() != ClubRole.PARENT) {
      throw new BadRequestException(
          msg("error.parentChild.parentRoleRequired"), "parentChild", "parentRoleRequired");
    }

    if (userRepository.existsParentChildRelationship(userId, dto.getParentId())) {
      throw new ConflictException(
          msg("error.parentChild.alreadyLinked"), "parentChild", "alreadyLinked");
    }

    child.getParents().add(parent);
    userRepository.save(child);

    // Sync conversations: add parent to child's team conversations
    List<TeamMember> childTeamMemberships = teamMemberRepository.findByUserId(userId);
    for (TeamMember tm : childTeamMemberships) {
      conversationService.addParticipantToTeamConversation(tm.getTeam().getId(), parent);
    }

    return userMapper.toDto(parent);
  }

  @Transactional
  public void unlinkParent(UUID clubId, UUID userId, UUID parentId) {
    User child = validateUserInClub(clubId, userId);

    if (!userRepository.existsParentChildRelationship(userId, parentId)) {
      throw new ResourceNotFoundException(
          msg("error.parentChild.notLinked"), "parentChild", parentId);
    }

    User parent = userRepository.findById(parentId).orElseThrow();
    child.getParents().remove(parent);
    userRepository.save(child);

    // Sync conversations: remove parent from child's team conversations
    // but only if parent has no other children in that team
    List<TeamMember> childTeamMemberships = teamMemberRepository.findByUserId(userId);
    List<User> parentOtherChildren = userRepository.findChildrenByParentId(parentId);

    for (TeamMember tm : childTeamMemberships) {
      UUID teamId = tm.getTeam().getId();
      boolean parentHasOtherChildInTeam =
          parentOtherChildren.stream()
              .filter(c -> !c.getId().equals(userId))
              .anyMatch(c -> teamMemberRepository.existsByTeamIdAndUserId(teamId, c.getId()));
      if (!parentHasOtherChildInTeam) {
        conversationService.removeParticipantFromTeamConversation(teamId, parentId);
      }
    }
  }

  @Transactional(readOnly = true)
  public List<UserDTO> listChildren(UUID clubId, UUID userId) {
    validateUserInClub(clubId, userId);
    return userRepository.findChildrenByParentId(userId).stream().map(userMapper::toDto).toList();
  }

  private User validateUserInClub(UUID clubId, UUID userId) {
    return userRepository
        .findByIdAndClubId(userId, clubId)
        .orElseThrow(
            () -> new ResourceNotFoundException(msg("error.user.notInClub"), "user", userId));
  }

  private String msg(String key, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(key, args, key, locale);
  }
}
