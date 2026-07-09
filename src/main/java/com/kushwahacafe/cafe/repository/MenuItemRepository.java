package com.kushwahacafe.cafe.repository;

import com.kushwahacafe.cafe.model.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByCategoryAndIsAvailable(String category, Boolean isAvailable);
}
