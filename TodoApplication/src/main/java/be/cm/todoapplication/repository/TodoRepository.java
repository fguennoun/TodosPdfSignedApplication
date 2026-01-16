package be.cm.todoapplication.repository;

import be.cm.todoapplication.model.Todo;
import be.cm.todoapplication.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TodoRepository extends JpaRepository<Todo, Long> {

    Page<Todo> findByUser(User user, Pageable pageable);

    // Méthode pour récupérer tous les todos d'un utilisateur sans pagination
    List<Todo> findByUser(User user);

    Page<Todo> findByUserAndCompleted(User user, Boolean completed, Pageable pageable);

    @Query("SELECT t FROM Todo t WHERE t.user = :user AND (:completed IS NULL OR t.completed = :completed)")
    Page<Todo> findByUserWithOptionalCompleted(@Param("user") User user,
                                               @Param("completed") Boolean completed,
                                               Pageable pageable);

    @Query("SELECT t FROM Todo t WHERE t.user = :user AND t.title LIKE %:search%")
    Page<Todo> findByUserAndTitleContaining(@Param("user") User user,
                                           @Param("search") String search,
                                           Pageable pageable);

    List<Todo> findByUserAndCompletedOrderByCreatedAtDesc(User user, Boolean completed);

    @Query("SELECT COUNT(t) FROM Todo t WHERE t.user = :user AND (:completed IS NULL OR t.completed = :completed)")
    long countByUserAndCompleted(@Param("user") User user, @Param("completed") Boolean completed);

    Optional<Todo> findByIdAndUser(Long id, User user);

    @Query("SELECT COUNT(t) FROM Todo t WHERE t.user = :user AND t.createdAt >= :since")
    long countByUserAndCreatedAtAfter(@Param("user") User user, @Param("since") LocalDateTime since);
}
