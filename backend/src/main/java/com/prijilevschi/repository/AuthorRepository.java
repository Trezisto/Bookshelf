package com.prijilevschi.repository;

import com.prijilevschi.entity.AuthorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuthorRepository extends JpaRepository<AuthorEntity, Long> {
    Optional<AuthorEntity> findByNameIgnoreCase(String name);

    List<AuthorEntity> findByNameContainingIgnoreCaseOrderByNameAsc(String part);

    List<AuthorEntity> findAllByOrderByNameAsc();
}
