package ee.finalthesis.clubmanagement.repository;

import ee.finalthesis.clubmanagement.domain.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  Page<User> findByClubId(UUID clubId, Pageable pageable);

  Optional<User> findByIdAndClubId(UUID userId, UUID clubId);

  Page<User> findByClubIdIsNull(Pageable pageable);

  @Query(
      "SELECT u FROM User u WHERE u.club IS NULL AND "
          + "(LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR "
          + "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR "
          + "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')))")
  Page<User> findUnaffiliatedBySearch(@Param("search") String search, Pageable pageable);

  @Query("SELECT p FROM User u JOIN u.parents p WHERE u.id = :childId")
  List<User> findParentsByChildId(@Param("childId") UUID childId);

  @Query("SELECT c FROM User c JOIN c.parents p WHERE p.id = :parentId")
  List<User> findChildrenByParentId(@Param("parentId") UUID parentId);

  @Query(
      "SELECT COUNT(u) > 0 FROM User u JOIN u.parents p WHERE u.id = :childId AND p.id = :parentId")
  boolean existsParentChildRelationship(
      @Param("childId") UUID childId, @Param("parentId") UUID parentId);
}
