package ee.finalthesis.clubmanagement.service;

import ee.finalthesis.clubmanagement.common.exception.BadRequestException;
import ee.finalthesis.clubmanagement.common.exception.ResourceNotFoundException;
import ee.finalthesis.clubmanagement.domain.Game;
import ee.finalthesis.clubmanagement.domain.GameSquadMember;
import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.domain.enumeration.AttendanceStatus;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.domain.enumeration.SystemRole;
import ee.finalthesis.clubmanagement.repository.GameRepository;
import ee.finalthesis.clubmanagement.repository.GameSquadMemberRepository;
import ee.finalthesis.clubmanagement.repository.UserRepository;
import ee.finalthesis.clubmanagement.security.SecurityUtils;
import ee.finalthesis.clubmanagement.service.dto.squad.AddSquadMembersDTO;
import ee.finalthesis.clubmanagement.service.dto.squad.SquadMemberDTO;
import ee.finalthesis.clubmanagement.service.dto.squad.SquadSummaryDTO;
import ee.finalthesis.clubmanagement.service.dto.squad.UpdateSquadMemberStatusDTO;
import ee.finalthesis.clubmanagement.service.mapper.GameMapper;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameSquadService {

  private final GameRepository gameRepository;
  private final GameSquadMemberRepository gameSquadMemberRepository;
  private final UserRepository userRepository;
  private final GameMapper gameMapper;
  private final MessageSource messageSource;

  @Transactional
  public List<SquadMemberDTO> addSquadMembers(
      UUID clubId, UUID gameId, AddSquadMembersDTO request) {
    Game game = findGameInClub(clubId, gameId);

    List<User> users = userRepository.findAllById(request.getUserIds());
    for (User user : users) {
      if (user.getClub() == null || !user.getClub().getId().equals(clubId)) {
        throw new BadRequestException(msg("error.squad.userNotInClub"), "squad", "userNotInClub");
      }
    }

    List<GameSquadMember> newMembers =
        users.stream()
            .filter(
                user -> !gameSquadMemberRepository.existsByGameIdAndUserId(gameId, user.getId()))
            .map(user -> GameSquadMember.builder().game(game).user(user).build())
            .toList();

    if (!newMembers.isEmpty()) {
      gameSquadMemberRepository.saveAll(newMembers);
    }

    return gameMapper.toSquadMemberDto(gameSquadMemberRepository.findByGameIdWithUser(gameId));
  }

  @Transactional
  public void removeSquadMember(UUID clubId, UUID gameId, UUID userId) {
    findGameInClub(clubId, gameId);

    GameSquadMember member =
        gameSquadMemberRepository
            .findByGameIdAndUserId(gameId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("GameSquadMember", "userId", userId));

    gameSquadMemberRepository.delete(member);
  }

  @Transactional(readOnly = true)
  public List<SquadMemberDTO> getSquadList(UUID clubId, UUID gameId) {
    findGameInClub(clubId, gameId);
    return gameMapper.toSquadMemberDto(gameSquadMemberRepository.findByGameIdWithUser(gameId));
  }

  @Transactional(readOnly = true)
  public SquadSummaryDTO getSquadSummary(UUID clubId, UUID gameId) {
    findGameInClub(clubId, gameId);
    List<GameSquadMember> members = gameSquadMemberRepository.findByGameIdWithUser(gameId);
    List<SquadMemberDTO> dtos = gameMapper.toSquadMemberDto(members);

    SquadSummaryDTO summary = new SquadSummaryDTO();
    summary.setTotal(dtos.size());
    summary.setConfirmed(
        (int) members.stream().filter(m -> m.getStatus() == AttendanceStatus.CONFIRMED).count());
    summary.setDeclined(
        (int) members.stream().filter(m -> m.getStatus() == AttendanceStatus.DECLINED).count());
    summary.setPending(
        (int) members.stream().filter(m -> m.getStatus() == AttendanceStatus.PENDING).count());
    summary.setMembers(dtos);
    return summary;
  }

  @Transactional
  public SquadMemberDTO updateSquadMemberStatus(
      UUID clubId, UUID gameId, UUID userId, UpdateSquadMemberStatusDTO request) {
    findGameInClub(clubId, gameId);

    GameSquadMember member =
        gameSquadMemberRepository
            .findByGameIdAndUserId(gameId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("GameSquadMember", "userId", userId));

    UUID currentUserId =
        SecurityUtils.getCurrentUserId()
            .orElseThrow(() -> new AccessDeniedException(msg("error.squad.notAuthorized")));

    if (!currentUserId.equals(userId)) {
      ClubRole clubRole = SecurityUtils.getCurrentUserRole().orElse(null);
      SystemRole systemRole = SecurityUtils.getCurrentUserSystemRole().orElse(null);
      boolean isAdminOrCoach =
          clubRole == ClubRole.CLUB_ADMIN
              || clubRole == ClubRole.COACH
              || systemRole == SystemRole.MASTER_ADMIN;

      if (!isAdminOrCoach) {
        User targetUser =
            userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        boolean isParent =
            targetUser.getParents().stream().anyMatch(p -> p.getId().equals(currentUserId));
        if (!isParent) {
          throw new AccessDeniedException(msg("error.squad.notAuthorized"));
        }
      }
    }

    member.setStatus(request.getStatus());
    member = gameSquadMemberRepository.save(member);
    return gameMapper.toSquadMemberDto(member);
  }

  private Game findGameInClub(UUID clubId, UUID gameId) {
    return gameRepository
        .findByIdAndClubId(gameId, clubId)
        .orElseThrow(() -> new ResourceNotFoundException("Game", "id", gameId));
  }

  private String msg(String key, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(key, args, key, locale);
  }
}
