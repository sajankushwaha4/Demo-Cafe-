package com.kushwahacafe.cafe.controller;

import com.kushwahacafe.cafe.model.CafeLocation;
import com.kushwahacafe.cafe.repository.CafeLocationRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.List;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private CafeLocationRepository locationRepository;

    @ModelAttribute("logged_in")
    public boolean isLoggedIn(HttpSession session) {
        return session.getAttribute("user_id") != null;
    }

    @ModelAttribute("username")
    public String getUsername(HttpSession session) {
        return (String) session.getAttribute("username");
    }

    @ModelAttribute("user_role")
    public String getUserRole(HttpSession session) {
        return (String) session.getAttribute("role");
    }

    @ModelAttribute("user_email")
    public String getUserEmail(HttpSession session) {
        return (String) session.getAttribute("email");
    }

    @ModelAttribute("cafe_locations")
    public List<CafeLocation> getCafeLocations() {
        return locationRepository.findAll();
    }

    @ModelAttribute("currentURI")
    public String getCurrentURI(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
