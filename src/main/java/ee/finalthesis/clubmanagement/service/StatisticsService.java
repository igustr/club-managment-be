package ee.finalthesis.clubmanagement.service;

import ee.finalthesis.clubmanagement.common.exception.ResourceNotFoundException;
import ee.finalthesis.clubmanagement.domain.Attendance;
import ee.finalthesis.clubmanagement.domain.Team;
import ee.finalthesis.clubmanagement.domain.enumeration.AttendanceStatus;
import ee.finalthesis.clubmanagement.repository.AttendanceRepository;
import ee.finalthesis.clubmanagement.repository.TeamMemberRepository;
import ee.finalthesis.clubmanagement.repository.TeamRepository;
import ee.finalthesis.clubmanagement.repository.TrainingSessionRepository;
import ee.finalthesis.clubmanagement.repository.UserRepository;
import ee.finalthesis.clubmanagement.service.dto.statistics.ClubStatisticsDTO;
import ee.finalthesis.clubmanagement.service.dto.statistics.MonthlyAttendanceDTO;
import ee.finalthesis.clubmanagement.service.dto.statistics.PlayerStatisticsDTO;
import ee.finalthesis.clubmanagement.service.dto.statistics.TeamStatisticsDTO;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatisticsService {

  private final AttendanceRepository attendanceRepository;
  private final TrainingSessionRepository trainingSessionRepository;
  private final TeamRepository teamRepository;
  private final TeamMemberRepository teamMemberRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public PlayerStatisticsDTO getPlayerStatistics(UUID clubId, UUID userId) {
    userRepository
        .findByIdAndClubId(userId, clubId)
        .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

    List<Attendance> attendances =
        attendanceRepository.findByUserIdAndTrainingSessionTeamClubId(userId, clubId);

    int total = attendances.size();
    int confirmed = (int) attendances.stream()
        .filter(a -> a.getStatus() == AttendanceStatus.CONFIRMED).count();
    int declined = (int) attendances.stream()
        .filter(a -> a.getStatus() == AttendanceStatus.DECLINED).count();
    int pending = (int) attendances.stream()
        .filter(a -> a.getStatus() == AttendanceStatus.PENDING).count();

    var user = userRepository.findByIdAndClubId(userId, clubId).get();

    return PlayerStatisticsDTO.builder()
        .userId(userId)
        .firstName(user.getFirstName())
        .lastName(user.getLastName())
        .totalTrainings(total)
        .confirmedCount(confirmed)
        .declinedCount(declined)
        .pendingCount(pending)
        .attendanceRate(total > 0 ? Math.round(confirmed * 1000.0 / total) / 10.0 : 0.0)
        .build();
  }

  @Transactional(readOnly = true)
  public TeamStatisticsDTO getTeamStatistics(UUID clubId, UUID teamId) {
    Team team = teamRepository
        .findByIdAndClubId(teamId, clubId)
        .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

    List<Attendance> teamAttendances =
        attendanceRepository.findByTrainingSessionTeamIdAndTrainingSessionTeamClubId(
            teamId, clubId);

    int totalTrainings = trainingSessionRepository.findByTeamId(teamId).size();
    int memberCount = teamMemberRepository.findByTeamId(teamId).size();

    // Group by user to get per-player stats
    Map<UUID, List<Attendance>> byUser = teamAttendances.stream()
        .collect(Collectors.groupingBy(a -> a.getUser().getId()));

    List<PlayerStatisticsDTO> playerStats = byUser.entrySet().stream()
        .map(entry -> {
          UUID userId = entry.getKey();
          List<Attendance> userAttendances = entry.getValue();
          int total = userAttendances.size();
          int confirmed = (int) userAttendances.stream()
              .filter(a -> a.getStatus() == AttendanceStatus.CONFIRMED).count();
          int declined = (int) userAttendances.stream()
              .filter(a -> a.getStatus() == AttendanceStatus.DECLINED).count();
          int pending = (int) userAttendances.stream()
              .filter(a -> a.getStatus() == AttendanceStatus.PENDING).count();
          var user = userAttendances.get(0).getUser();

          return PlayerStatisticsDTO.builder()
              .userId(userId)
              .firstName(user.getFirstName())
              .lastName(user.getLastName())
              .totalTrainings(total)
              .confirmedCount(confirmed)
              .declinedCount(declined)
              .pendingCount(pending)
              .attendanceRate(total > 0 ? Math.round(confirmed * 1000.0 / total) / 10.0 : 0.0)
              .build();
        })
        .sorted(Comparator.comparing(PlayerStatisticsDTO::getLastName))
        .toList();

    double avgRate = playerStats.isEmpty() ? 0.0
        : Math.round(playerStats.stream()
            .mapToDouble(PlayerStatisticsDTO::getAttendanceRate)
            .average().orElse(0.0) * 10.0) / 10.0;

    return TeamStatisticsDTO.builder()
        .teamId(teamId)
        .teamName(team.getName())
        .memberCount(memberCount)
        .totalTrainings(totalTrainings)
        .averageAttendanceRate(avgRate)
        .playerStatistics(playerStats)
        .build();
  }

  @Transactional(readOnly = true)
  public ClubStatisticsDTO getClubStatistics(UUID clubId) {
    long totalMembers = userRepository.countByClubId(clubId);
    List<Team> teams = teamRepository.findByClubId(clubId);
    int totalTrainings = trainingSessionRepository.findByTeamClubId(clubId).size();

    List<Attendance> allAttendances = attendanceRepository.findByTrainingSessionTeamClubId(clubId);

    // Overall attendance rate
    long totalConfirmed = allAttendances.stream()
        .filter(a -> a.getStatus() == AttendanceStatus.CONFIRMED).count();
    double overallRate = allAttendances.isEmpty() ? 0.0
        : Math.round(totalConfirmed * 1000.0 / allAttendances.size()) / 10.0;

    // Per-team statistics (summary only, no player details)
    List<TeamStatisticsDTO> teamStats = teams.stream()
        .map(team -> {
          List<Attendance> teamAttendances = allAttendances.stream()
              .filter(a -> a.getTrainingSession().getTeam().getId().equals(team.getId()))
              .toList();

          int teamTrainings = trainingSessionRepository.findByTeamId(team.getId()).size();
          int memberCount = teamMemberRepository.findByTeamId(team.getId()).size();

          long teamConfirmed = teamAttendances.stream()
              .filter(a -> a.getStatus() == AttendanceStatus.CONFIRMED).count();
          double teamRate = teamAttendances.isEmpty() ? 0.0
              : Math.round(teamConfirmed * 1000.0 / teamAttendances.size()) / 10.0;

          return TeamStatisticsDTO.builder()
              .teamId(team.getId())
              .teamName(team.getName())
              .memberCount(memberCount)
              .totalTrainings(teamTrainings)
              .averageAttendanceRate(teamRate)
              .build();
        })
        .sorted(Comparator.comparing(TeamStatisticsDTO::getTeamName))
        .toList();

    // Monthly attendance (last 12 months)
    DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
    Map<String, List<Attendance>> byMonth = allAttendances.stream()
        .collect(Collectors.groupingBy(
            a -> a.getTrainingSession().getDate().format(monthFormatter)));

    List<MonthlyAttendanceDTO> monthlyAttendance = byMonth.entrySet().stream()
        .map(entry -> {
          List<Attendance> monthAttendances = entry.getValue();
          long monthConfirmed = monthAttendances.stream()
              .filter(a -> a.getStatus() == AttendanceStatus.CONFIRMED).count();
          long monthTrainings = monthAttendances.stream()
              .map(a -> a.getTrainingSession().getId())
              .distinct().count();

          return MonthlyAttendanceDTO.builder()
              .month(entry.getKey())
              .totalTrainings((int) monthTrainings)
              .totalAttendances(monthAttendances.size())
              .confirmedCount((int) monthConfirmed)
              .attendanceRate(monthAttendances.isEmpty() ? 0.0
                  : Math.round(monthConfirmed * 1000.0 / monthAttendances.size()) / 10.0)
              .build();
        })
        .sorted(Comparator.comparing(MonthlyAttendanceDTO::getMonth))
        .toList();

    return ClubStatisticsDTO.builder()
        .totalMembers((int) totalMembers)
        .totalTeams(teams.size())
        .totalTrainings(totalTrainings)
        .overallAttendanceRate(overallRate)
        .teamStatistics(teamStats)
        .monthlyAttendance(monthlyAttendance)
        .build();
  }
}
