package com.prijilevschi.repository;

import com.prijilevschi.entity.ShelfEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShelfRepository extends JpaRepository<ShelfEntity, Long> {
    List<ShelfEntity> findAllByOrderByLocationAscRowNumAsc();

    boolean existsByLocationIgnoreCaseAndRowNumAndIdNot(String location, int rowNum, Long id);

    boolean existsByLocationIgnoreCaseAndRowNum(String location, int rowNum);
}
