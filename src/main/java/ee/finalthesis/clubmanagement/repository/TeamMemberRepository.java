package ee.finalthesis.clubmanagement.repository;

import ee.finalthesis.clubmanagement.domain.TeamMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

  boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);

  boolean existsByTeamClubIdAndUserId(UUID clubId, UUID userId);

  List<TeamMember> findByTeamId(UUID teamId);

  Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);

  List<TeamMember> findByUserId(UUID userId);

  void deleteByTeamIdAndUserId(UUID teamId, UUID userId);

  void deleteByUserId(UUID userId);

  long countByTeamId(UUID teamId);

  @Query(
      "SELECT tm.team.id, COUNT(tm) FROM TeamMember tm"
          + " WHERE tm.team.club.id = :clubId GROUP BY tm.team.id")
  List<Object[]> countByTeamIdGroupByTeam(@Param("clubId") UUID clubId);

  @Query(
      "SELECT tm.team.id FROM TeamMember tm"
          + " WHERE tm.user.id IN :userIds AND tm.team.id IN :teamIds")
  List<UUID> findTeamIdsByUserIdsAndTeamIds(
      @Param("userIds") List<UUID> userIds, @Param("teamIds") List<UUID> teamIds);

  @Query(
      "SELECT DISTINCT tm FROM TeamMember tm"
          + " JOIN FETCH tm.user u"
          + " LEFT JOIN FETCH u.parents p"
          + " LEFT JOIN FETCH u.club"
          + " LEFT JOIN FETCH p.club"
          + " WHERE tm.team.id = :teamId")
  List<TeamMember> findByTeamIdWithUsersAndParents(@Param("teamId") UUID teamId);
}
