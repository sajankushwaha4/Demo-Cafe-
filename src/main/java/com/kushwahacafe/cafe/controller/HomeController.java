package com.kushwahacafe.cafe.controller;

import com.kushwahacafe.cafe.model.MenuItem;
import com.kushwahacafe.cafe.repository.MenuItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    @Autowired
    private MenuItemRepository menuItemRepository;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/menu")
    public String menu(Model model) {
        List<String> categories = Arrays.asList("Hot Drinks", "Cold Drinks", "Snacks", "Pizza", "Burger", "Desserts");
        Map<String, List<MenuItem>> itemsByCategory = new LinkedHashMap<>();

        for (String cat : categories) {
            itemsByCategory.put(cat, menuItemRepository.findByCategoryAndIsAvailable(cat, true));
        }

        model.addAttribute("items_by_category", itemsByCategory);
        return "menu";
    }
}
