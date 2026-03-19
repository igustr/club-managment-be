package ee.finalthesis.clubmanagement.service;

import ee.finalthesis.clubmanagement.common.exception.BadRequestException;
import ee.finalthesis.clubmanagement.common.exception.ResourceNotFoundException;
import ee.finalthesis.clubmanagement.domain.Tournament;
import ee.finalthesis.clubmanagement.domain.TournamentSquadMember;
import ee.finalthesis.clubmanagement.domain.User;
import ee.finalthesis.clubmanagement.domain.enumeration.AttendanceStatus;
import ee.finalthesis.clubmanagement.domain.enumeration.ClubRole;
import ee.finalthesis.clubmanagement.domain.enumeration.SystemRole;
import ee.finalthesis.clubmanagement.repository.TournamentRepository;
import ee.finalthesis.clubmanagement.repository.TournamentSquadMemberRepository;
import ee.finalthesis.clubmanagement.repository.UserRepository;
import ee.finalthesis.clubmanagement.security.SecurityUtils;
import ee.finalthesis.clubmanagement.service.dto.squad.AddSquadMembersDTO;
import ee.finalthesis.clubmanagement.service.dto.squad.SquadMemberDTO;
import ee.finalthesis.clubmanagement.service.dto.squad.SquadSummaryDTO;
import ee.finalthesis.clubmanagement.service.dto.squad.UpdateSquadMemberStatusDTO;
import ee.finalthesis.clubmanagement.service.mapper.TournamentMapper;
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
public class TournamentSquadService {

  private final TournamentRepository tournamentRepository;
  private final TournamentSquadMemberRepository tournamentSquadMemberRepository;
  private final UserRepository userRepository;
  private final TournamentMapper tournamentMapper;
  private final MessageSource messageSource;

  @Transactional
  public List<SquadMemberDTO> addSquadMembers(
      UUID clubId, UUID tournamentId, AddSquadMembersDTO request) {
    Tournament tournament = findTournamentInClub(clubId, tournamentId);

    List<User> users = userRepository.findAllById(request.getUserIds());
    for (User user : users) {
      if (user.getClub() == null || !user.getClub().getId().equals(clubId)) {
        throw new BadRequestException(msg("error.squad.userNotInClub"), "squad", "userNotInClub");
      }
    }

    List<TournamentSquadMember> newMembers =
        users.stream()
            .filter(
                user ->
                    !tournamentSquadMemberRepository.existsByTournamentIdAndUserId(
                        tournamentId, user.getId()))
            .map(user -> TournamentSquadMember.builder().tournament(tournament).user(user).build())
            .toList();

    if (!newMembers.isEmpty()) {
      tournamentSquadMemberRepository.saveAll(newMembers);
    }

    return tournamentMapper.toSquadMemberDto(
        tournamentSquadMemberRepository.findByTournamentIdWithUser(tournamentId));
  }

  @Transactional
  public void removeSquadMember(UUID clubId, UUID tournamentId, UUID userId) {
    findTournamentInClub(clubId, tournamentId);

    TournamentSquadMember member =
        tournamentSquadMemberRepository
            .findByTournamentIdAndUserId(tournamentId, userId)
            .orElseThrow(
                () -> new ResourceNotFoundException("TournamentSquadMember", "userId", userId));

    tournamentSquadMemberRepository.delete(member);
  }

  @Transactional(readOnly = true)
  public List<SquadMemberDTO> getSquadList(UUID clubId, UUID tournamentId) {
    findTournamentInClub(clubId, tournamentId);
    return tournamentMapper.toSquadMemberDto(
        tournamentSquadMemberRepository.findByTournamentIdWithUser(tournamentId));
  }

  @Transactional(readOnly = true)
  public SquadSummaryDTO getSquadSummary(UUID clubId, UUID tournamentId) {
    findTournamentInClub(clubId, tournamentId);
    List<TournamentSquadMember> members =
        tournamentSquadMemberRepository.findByTournamentIdWithUser(tournamentId);
    List<SquadMemberDTO> dtos = tournamentMapper.toSquadMemberDto(members);

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
      UUID clubId, UUID tournamentId, UUID userId, UpdateSquadMemberStatusDTO request) {
    findTournamentInClub(clubId, tournamentId);

    TournamentSquadMember member =
        tournamentSquadMemberRepository
            .findByTournamentIdAndUserId(tournamentId, userId)
            .orElseThrow(
                () -> new ResourceNotFoundException("TournamentSquadMember", "userId", userId));

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
    member = tournamentSquadMemberRepository.save(member);
    return tournamentMapper.toSquadMemberDto(member);
  }

  private Tournament findTournamentInClub(UUID clubId, UUID tournamentId) {
    return tournamentRepository
        .findByIdAndClubId(tournamentId, clubId)
        .orElseThrow(() -> new ResourceNotFoundException("Tournament", "id", tournamentId));
  }

  private String msg(String key, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    return messageSource.getMessage(key, args, key, locale);
  }
}
