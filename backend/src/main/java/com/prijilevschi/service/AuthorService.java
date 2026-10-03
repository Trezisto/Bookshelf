package com.prijilevschi.service;

import com.prijilevschi.dto.AuthorDTO;
import com.prijilevschi.entity.AuthorEntity;
import com.prijilevschi.repository.AuthorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AuthorService {
    private final AuthorRepository authorRepository;

    public AuthorService(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @Transactional(readOnly = true)
    public List<AuthorDTO> find(String query) {
        List<AuthorEntity> authors = query == null || query.isBlank()
                ? authorRepository.findAllByOrderByNameAsc()
                : authorRepository.findByNameContainingIgnoreCaseOrderByNameAsc(query.strip());
        return authors.stream().map(Mapper::toDto).toList();
    }

    /** Authors are matched by name, ignoring case; a new name creates a new author. */
    public AuthorEntity findOrCreate(String name) {
        String clean = name.strip().replaceAll("\\s+", " ");
        return authorRepository.findByNameIgnoreCase(clean)
                .orElseGet(() -> authorRepository.save(new AuthorEntity(clean)));
    }
}
