package ee.finalthesis.clubmanagement.repository;

import ee.finalthesis.clubmanagement.domain.GameSquadMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GameSquadMemberRepository extends JpaRepository<GameSquadMember, UUID> {

  @Query("SELECT sm FROM GameSquadMember sm JOIN FETCH sm.user" + " WHERE sm.game.id = :gameId")
  List<GameSquadMember> findByGameIdWithUser(@Param("gameId") UUID gameId);

  Optional<GameSquadMember> findByGameIdAndUserId(UUID gameId, UUID userId);

  boolean existsByGameIdAndUserId(UUID gameId, UUID userId);

  void deleteByGameId(UUID gameId);

  @Query(
      "SELECT DISTINCT sm.game.id FROM GameSquadMember sm"
          + " WHERE sm.user.id = :userId AND sm.game.club.id = :clubId")
  List<UUID> findGameIdsByUserIdAndClubId(
      @Param("userId") UUID userId, @Param("clubId") UUID clubId);
}
