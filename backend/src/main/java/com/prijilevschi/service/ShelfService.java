package com.prijilevschi.service;

import com.prijilevschi.dto.BookDTO;
import com.prijilevschi.dto.PositionDTO;
import com.prijilevschi.dto.ShelfDTO;
import com.prijilevschi.dto.ShelfRequest;
import com.prijilevschi.entity.BookEntity;
import com.prijilevschi.entity.ShelfEntity;
import com.prijilevschi.error.ConflictException;
import com.prijilevschi.error.NotFoundException;
import com.prijilevschi.repository.BookRepository;
import com.prijilevschi.repository.ShelfRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ShelfService {
    private final ShelfRepository shelfRepository;
    private final BookRepository bookRepository;

    public ShelfService(ShelfRepository shelfRepository, BookRepository bookRepository) {
        this.shelfRepository = shelfRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional(readOnly = true)
    public List<ShelfDTO> findAll() {
        return shelfRepository.findAllByOrderByLocationAscRowNumAsc().stream().map(Mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ShelfDTO get(Long id) {
        return Mapper.toDto(getEntity(id));
    }

    public ShelfDTO create(ShelfRequest request) {
        if (shelfRepository.existsByLocationIgnoreCaseAndRowNum(request.location().strip(), request.rowNum())) {
            throw duplicate(request);
        }
        ShelfEntity shelf = new ShelfEntity();
        apply(shelf, request);
        return Mapper.toDto(shelfRepository.save(shelf));
    }

    public ShelfDTO update(Long id, ShelfRequest request) {
        ShelfEntity shelf = getEntity(id);
        if (shelfRepository.existsByLocationIgnoreCaseAndRowNumAndIdNot(request.location().strip(), request.rowNum(), id)) {
            throw duplicate(request);
        }
        apply(shelf, request);
        return Mapper.toDto(shelf);
    }

    /** Books on a deleted shelf are kept and become "unshelved". */
    public void delete(Long id) {
        ShelfEntity shelf = getEntity(id);
        bookRepository.unassignShelf(id);
        shelfRepository.delete(shelf);
    }

    @Transactional(readOnly = true)
    public List<BookDTO> books(Long id) {
        getEntity(id);
        return bookRepository.findByShelfIdOrderByDepthRowAscPositionNumberAsc(id).stream().map(Mapper::toDto).toList();
    }

    /** The first free slot (counted from 1, from the left) in the given depth row. */
    @Transactional(readOnly = true)
    public PositionDTO nextPosition(Long id, int depthRow) {
        getEntity(id);
        Set<Integer> taken = bookRepository.findByShelfIdAndDepthRow(id, depthRow).stream()
                .map(BookEntity::getPositionNumber)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        int position = 1;
        while (taken.contains(position)) {
            position++;
        }
        return new PositionDTO(id, depthRow, position);
    }

    ShelfEntity getEntity(Long id) {
        return shelfRepository.findById(id).orElseThrow(() -> new NotFoundException("Shelf " + id + " not found"));
    }

    private static void apply(ShelfEntity shelf, ShelfRequest request) {
        shelf.setLocation(request.location().strip());
        shelf.setRowNum(request.rowNum());
        shelf.setOrientation(request.orientation());
    }

    private static ConflictException duplicate(ShelfRequest request) {
        return new ConflictException("Shelf '" + request.location().strip() + "' row " + request.rowNum() + " already exists");
    }
}
