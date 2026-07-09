package com.kushwahacafe.cafe.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kushwahacafe.cafe.model.Order;
import com.kushwahacafe.cafe.model.Reservation;
import com.kushwahacafe.cafe.model.User;
import com.kushwahacafe.cafe.repository.OrderRepository;
import com.kushwahacafe.cafe.repository.ReservationRepository;
import com.kushwahacafe.cafe.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private OrderRepository orderRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Long userId = (Long) session.getAttribute("user_id");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("flashMessage", "Please login to view your profile.");
            redirectAttributes.addFlashAttribute("flashCategory", "error");
            return "redirect:/login";
        }

        releaseExpiredReservations();

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            session.invalidate();
            return "redirect:/login";
        }
        User user = userOpt.get();

        List<Reservation> userReservations = reservationRepository.findByUserOrderByCreatedAtDesc(user);
        List<Order> userOrders = orderRepository.findByUserOrderByCreatedAtDesc(user);

        List<Map<String, Object>> ordersData = new ArrayList<>();
        for (Order order : userOrders) {
            List<Map<String, Object>> itemsList;
            try {
                itemsList = objectMapper.readValue(order.getItems(), new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception e) {
                itemsList = new ArrayList<>();
            }
            Map<String, Object> orderMap = new HashMap<>();
            orderMap.put("order", order);
            orderMap.put("items_list", itemsList);
            ordersData.add(orderMap);
        }

        model.addAttribute("user", user);
        model.addAttribute("reservations", userReservations);
        model.addAttribute("orders_data", ordersData);

        return "profile";
    }

    public void releaseExpiredReservations() {
        LocalDate now = LocalDate.now();
        LocalTime nowTime = LocalTime.now();
        String currentDateStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String currentTimeStr = nowTime.format(DateTimeFormatter.ofPattern("HH:mm"));

        List<Reservation> activeReservations = reservationRepository.findByStatus("Active");
        int updatedCount = 0;

        for (Reservation res : activeReservations) {
            try {
                // Check if date has passed or if it is today and time has passed slot_out
                if (res.getDate().compareTo(currentDateStr) < 0 ||
                    (res.getDate().equals(currentDateStr) && currentTimeStr.compareTo(res.getSlotOut()) >= 0)) {
                    res.setStatus("Released");
                    reservationRepository.save(res);
                    updatedCount++;
                }
            } catch (Exception e) {
                System.err.println("[AUTO-RELEASE ERROR] Failed to process reservation " + res.getId() + ": " + e.getMessage());
            }
        }

        if (updatedCount > 0) {
            System.out.println("[AUTO-RELEASE] Automatically released " + updatedCount + " expired table slots.");
        }
    }
}
