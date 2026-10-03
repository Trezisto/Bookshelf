package com.prijilevschi.controller;

import com.prijilevschi.dto.BookDTO;
import com.prijilevschi.dto.PositionDTO;
import com.prijilevschi.dto.ShelfDTO;
import com.prijilevschi.dto.ShelfRequest;
import com.prijilevschi.service.ShelfService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shelves")
public class ShelfController {
    private final ShelfService shelfService;

    public ShelfController(ShelfService shelfService) {
        this.shelfService = shelfService;
    }

    @GetMapping
    public List<ShelfDTO> findAll() {
        return shelfService.findAll();
    }

    @GetMapping("/{id}")
    public ShelfDTO get(@PathVariable Long id) {
        return shelfService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShelfDTO create(@Valid @RequestBody ShelfRequest request) {
        return shelfService.create(request);
    }

    @PutMapping("/{id}")
    public ShelfDTO update(@PathVariable Long id, @Valid @RequestBody ShelfRequest request) {
        return shelfService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        shelfService.delete(id);
    }

    @GetMapping("/{id}/books")
    public List<BookDTO> books(@PathVariable Long id) {
        return shelfService.books(id);
    }

    @GetMapping("/{id}/next-position")
    public PositionDTO nextPosition(@PathVariable Long id, @RequestParam(defaultValue = "1") int depthRow) {
        return shelfService.nextPosition(id, depthRow);
    }
}
