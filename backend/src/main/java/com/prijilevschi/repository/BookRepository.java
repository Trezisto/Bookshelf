package com.prijilevschi.repository;

import com.prijilevschi.entity.BookEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<BookEntity, Long> {

    /**
     * Combined search used by the shelves screen. Every filter is optional:
     * {@code pattern} (lower-case, already wrapped in %) matches the title or the name of the author or a co-author,
     * {@code isbn} (normalised) matches exactly.
     */
    @Query("""
            select b from BookEntity b
              join fetch b.author a
              left join fetch b.shelf s
            where (:pattern is null or lower(b.name) like :pattern or lower(a.name) like :pattern
                   or exists (select 1 from b.coAuthors c where lower(c) like :pattern)
                   or (:isbn is not null and b.isbn = :isbn))
              and (:shelfId is null or s.id = :shelfId)
              and (:read is null or b.read = :read)
              and (:language is null or lower(b.language) = :language)
            order by s.location, s.rowNum, b.depthRow, b.positionNumber, b.name
            """)
    List<BookEntity> search(@Param("pattern") String pattern,
                            @Param("isbn") String isbn,
                            @Param("shelfId") Long shelfId,
                            @Param("read") Boolean read,
                            @Param("language") String language);

    @EntityGraph(attributePaths = {"author", "shelf"})
    Optional<BookEntity> findWithAuthorAndShelfById(Long id);

    @EntityGraph(attributePaths = {"author", "shelf"})
    List<BookEntity> findByShelfIdOrderByDepthRowAscPositionNumberAsc(Long shelfId);

    List<BookEntity> findByShelfIdAndDepthRow(Long shelfId, int depthRow);

    Optional<BookEntity> findFirstByShelfIdAndDepthRowAndPositionNumber(Long shelfId, int depthRow, int positionNumber);

    Optional<BookEntity> findByIsbn(String isbn);

    @Query("select distinct b.language from BookEntity b where b.language is not null order by b.language")
    List<String> findDistinctLanguages();

    @Modifying
    @Query("update BookEntity b set b.shelf = null, b.positionNumber = null where b.shelf.id = :shelfId")
    int unassignShelf(@Param("shelfId") Long shelfId);
}
