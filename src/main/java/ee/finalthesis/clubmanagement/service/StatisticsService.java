package ee.finalthesis.clubmanagement.service;

import ee.finalthesis.clubmanagement.common.exception.ResourceNotFoundException;
import ee.finalthesis.clubmanagement.domain.Attendance;
import ee.finalthesis.clubmanagement.domain.Team;
import ee.finalthesis.clubmanagement.domain.User;
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
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
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
    User user =
        userRepository
            .findByIdAndClubId(userId, clubId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

    Map<AttendanceStatus, Long> statusCounts =
        toStatusMap(attendanceRepository.countByStatusForUserAndClub(userId, clubId));

    int confirmed = statusCounts.getOrDefault(AttendanceStatus.CONFIRMED, 0L).intValue();
    int declined = statusCounts.getOrDefault(AttendanceStatus.DECLINED, 0L).intValue();
    int pending = statusCounts.getOrDefault(AttendanceStatus.PENDING, 0L).intValue();
    int total = confirmed + declined + pending;

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
    Team team =
        teamRepository
            .findByIdAndClubId(teamId, clubId)
            .orElseThrow(() -> new ResourceNotFoundException("Team", "id", teamId));

    long totalTrainings = trainingSessionRepository.countByTeamId(teamId);
    long memberCount = teamMemberRepository.countByTeamId(teamId);

    // Load all attendance for this team and group by user in Java
    List<Attendance> teamAttendances =
        attendanceRepository.findByTrainingSessionTeamIdAndTrainingSessionTeamClubId(
            teamId, clubId);

    Map<UUID, List<Attendance>> byUser =
        teamAttendances.stream().collect(Collectors.groupingBy(a -> a.getUser().getId()));

    List<PlayerStatisticsDTO> playerStats =
        byUser.entrySet().stream()
            .map(
                entry -> {
                  UUID uid = entry.getKey();
                  List<Attendance> userAttendances = entry.getValue();
                  int total = userAttendances.size();
                  int confirmed =
                      (int)
                          userAttendances.stream()
                              .filter(a -> a.getStatus() == AttendanceStatus.CONFIRMED)
                              .count();
                  int declined =
                      (int)
                          userAttendances.stream()
                              .filter(a -> a.getStatus() == AttendanceStatus.DECLINED)
                              .count();
                  int pending =
                      (int)
                          userAttendances.stream()
                              .filter(a -> a.getStatus() == AttendanceStatus.PENDING)
                              .count();
                  var u = userAttendances.get(0).getUser();

                  return PlayerStatisticsDTO.builder()
                      .userId(uid)
                      .firstName(u.getFirstName())
                      .lastName(u.getLastName())
                      .totalTrainings(total)
                      .confirmedCount(confirmed)
                      .declinedCount(declined)
                      .pendingCount(pending)
                      .attendanceRate(
                          total > 0 ? Math.round(confirmed * 1000.0 / total) / 10.0 : 0.0)
                      .build();
                })
            .sorted(Comparator.comparing(PlayerStatisticsDTO::getLastName))
            .toList();

    double avgRate =
        playerStats.isEmpty()
            ? 0.0
            : Math.round(
                    playerStats.stream()
                            .mapToDouble(PlayerStatisticsDTO::getAttendanceRate)
                            .average()
                            .orElse(0.0)
                        * 10.0)
                / 10.0;

    return TeamStatisticsDTO.builder()
        .teamId(teamId)
        .teamName(team.getName())
        .memberCount((int) memberCount)
        .totalTrainings((int) totalTrainings)
        .averageAttendanceRate(avgRate)
        .playerStatistics(playerStats)
        .build();
  }

  @Transactional(readOnly = true)
  public ClubStatisticsDTO getClubStatistics(UUID clubId) {
    long totalMembers = userRepository.countByClubId(clubId);
    List<Team> teams = teamRepository.findByClubId(clubId);
    long totalTrainings = trainingSessionRepository.countByTeamClubId(clubId);

    // Club-wide attendance counts via GROUP BY
    Map<AttendanceStatus, Long> clubStatusCounts =
        toStatusMap(attendanceRepository.countByStatusForClub(clubId));
    long totalConfirmed = clubStatusCounts.getOrDefault(AttendanceStatus.CONFIRMED, 0L);
    long totalAttendances = clubStatusCounts.values().stream().mapToLong(Long::longValue).sum();
    double overallRate =
        totalAttendances == 0 ? 0.0 : Math.round(totalConfirmed * 1000.0 / totalAttendances) / 10.0;

    // Per-team training counts (single query)
    Map<UUID, Long> trainingCountsByTeam = new HashMap<>();
    for (Object[] row : trainingSessionRepository.countByTeamIdGroupByTeam(clubId)) {
      trainingCountsByTeam.put((UUID) row[0], (Long) row[1]);
    }

    // Per-team member counts (single query)
    Map<UUID, Long> memberCountsByTeam = new HashMap<>();
    for (Object[] row : teamMemberRepository.countByTeamIdGroupByTeam(clubId)) {
      memberCountsByTeam.put((UUID) row[0], (Long) row[1]);
    }

    // Per-team attendance by status (single query)
    // Map: teamId -> status -> count
    Map<UUID, Map<AttendanceStatus, Long>> teamAttendanceCounts = new HashMap<>();
    for (Object[] row : attendanceRepository.countByTeamAndStatusForClub(clubId)) {
      UUID teamId = (UUID) row[0];
      AttendanceStatus status = (AttendanceStatus) row[1];
      long count = (Long) row[2];
      teamAttendanceCounts
          .computeIfAbsent(teamId, k -> new EnumMap<>(AttendanceStatus.class))
          .put(status, count);
    }

    List<TeamStatisticsDTO> teamStats =
        teams.stream()
            .map(
                team -> {
                  long teamTrainings = trainingCountsByTeam.getOrDefault(team.getId(), 0L);
                  long members = memberCountsByTeam.getOrDefault(team.getId(), 0L);
                  Map<AttendanceStatus, Long> statusMap =
                      teamAttendanceCounts.getOrDefault(
                          team.getId(), new EnumMap<>(AttendanceStatus.class));
                  long teamTotal = statusMap.values().stream().mapToLong(Long::longValue).sum();
                  long teamConfirmed = statusMap.getOrDefault(AttendanceStatus.CONFIRMED, 0L);
                  double teamRate =
                      teamTotal == 0 ? 0.0 : Math.round(teamConfirmed * 1000.0 / teamTotal) / 10.0;

                  return TeamStatisticsDTO.builder()
                      .teamId(team.getId())
                      .teamName(team.getName())
                      .memberCount((int) members)
                      .totalTrainings((int) teamTrainings)
                      .averageAttendanceRate(teamRate)
                      .build();
                })
            .sorted(Comparator.comparing(TeamStatisticsDTO::getTeamName))
            .toList();

    // Monthly attendance via GROUP BY
    Map<String, Map<AttendanceStatus, Long>> monthlyStatusCounts = new HashMap<>();
    for (Object[] row : attendanceRepository.countByMonthAndStatusForClub(clubId)) {
      String month = (String) row[0];
      AttendanceStatus status = (AttendanceStatus) row[1];
      long count = (Long) row[2];
      monthlyStatusCounts
          .computeIfAbsent(month, k -> new EnumMap<>(AttendanceStatus.class))
          .put(status, count);
    }

    List<MonthlyAttendanceDTO> monthlyAttendance =
        monthlyStatusCounts.entrySet().stream()
            .map(
                entry -> {
                  String month = entry.getKey();
                  Map<AttendanceStatus, Long> statusMap = entry.getValue();
                  long monthTotal = statusMap.values().stream().mapToLong(Long::longValue).sum();
                  long monthConfirmed = statusMap.getOrDefault(AttendanceStatus.CONFIRMED, 0L);
                  int monthTrainings =
                      attendanceRepository.countDistinctTrainingSessionsByClubAndMonth(
                          clubId, month);

                  return MonthlyAttendanceDTO.builder()
                      .month(month)
                      .totalTrainings(monthTrainings)
                      .totalAttendances((int) monthTotal)
                      .confirmedCount((int) monthConfirmed)
                      .attendanceRate(
                          monthTotal == 0
                              ? 0.0
                              : Math.round(monthConfirmed * 1000.0 / monthTotal) / 10.0)
                      .build();
                })
            .sorted(Comparator.comparing(MonthlyAttendanceDTO::getMonth))
            .toList();

    return ClubStatisticsDTO.builder()
        .totalMembers((int) totalMembers)
        .totalTeams(teams.size())
        .totalTrainings((int) totalTrainings)
        .overallAttendanceRate(overallRate)
        .teamStatistics(teamStats)
        .monthlyAttendance(monthlyAttendance)
        .build();
  }

  private Map<AttendanceStatus, Long> toStatusMap(List<Object[]> rows) {
    Map<AttendanceStatus, Long> map = new EnumMap<>(AttendanceStatus.class);
    for (Object[] row : rows) {
      map.put((AttendanceStatus) row[0], (Long) row[1]);
    }
    return map;
  }
}
