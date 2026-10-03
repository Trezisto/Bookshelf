package com.prijilevschi.service;

import com.prijilevschi.dto.BookRevisionDTO;
import com.prijilevschi.entity.BookEntity;
import com.prijilevschi.entity.BookRevisionEntity;
import com.prijilevschi.error.NotFoundException;
import com.prijilevschi.repository.BookRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Reads a book's change history from the Envers audit tables, oldest revision first. */
@Service
@Transactional(readOnly = true)
public class BookHistoryService {

    private final EntityManager entityManager;
    private final BookRepository bookRepository;

    public BookHistoryService(EntityManager entityManager, BookRepository bookRepository) {
        this.entityManager = entityManager;
        this.bookRepository = bookRepository;
    }

    @SuppressWarnings("unchecked")
    public List<BookRevisionDTO> history(Long bookId) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        List<Object[]> rows = reader.createQuery()
                .forRevisionsOfEntity(BookEntity.class, false, true)
                .add(AuditEntity.id().eq(bookId))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();
        if (rows.isEmpty() && !bookRepository.existsById(bookId)) {
            throw new NotFoundException("Book " + bookId + " not found");
        }
        return rows.stream().map(row -> {
            BookEntity book = (BookEntity) row[0];
            BookRevisionEntity revision = (BookRevisionEntity) row[1];
            RevisionType type = (RevisionType) row[2];
            boolean deleted = type == RevisionType.DEL;
            return new BookRevisionDTO(revision.getId(), revision.getTimestamp(), type.name(),
                    deleted ? null : book.getName(),
                    deleted ? null : book.getIsbn(),
                    deleted ? null : book.getPublisher(),
                    deleted ? null : book.getPublicationDate(),
                    deleted ? null : book.getRating(),
                    deleted ? null : book.isRead());
        }).toList();
    }
}
