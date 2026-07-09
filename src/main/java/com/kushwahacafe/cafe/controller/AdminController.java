package com.kushwahacafe.cafe.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kushwahacafe.cafe.model.CafeLocation;
import com.kushwahacafe.cafe.model.MenuItem;
import com.kushwahacafe.cafe.model.Order;
import com.kushwahacafe.cafe.model.Reservation;
import com.kushwahacafe.cafe.repository.CafeLocationRepository;
import com.kushwahacafe.cafe.repository.MenuItemRepository;
import com.kushwahacafe.cafe.repository.OrderRepository;
import com.kushwahacafe.cafe.repository.ReservationRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.*;

@Controller
public class AdminController {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private CafeLocationRepository locationRepository;

    @Autowired
    private ProfileController profileController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private boolean isAdmin(HttpSession session) {
        String role = (String) session.getAttribute("role");
        return "admin".equals(role);
    }

    @GetMapping("/admin")
    public String adminDashboard(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!isAdmin(session)) {
            redirectAttributes.addFlashAttribute("flashMessage", "Unauthorized. Admins only.");
            redirectAttributes.addFlashAttribute("flashCategory", "error");
            return "redirect:/login";
        }

        profileController.releaseExpiredReservations();

        // Statistics
        double totalSales = orderRepository.findAll().stream()
                .filter(o -> "Paid".equals(o.getPaymentStatus()))
                .mapToDouble(Order::getTotalAmount)
                .sum();
        long totalBookings = reservationRepository.count();
        long totalOrders = orderRepository.countByPaymentStatus("Paid");

        List<Reservation> reservations = reservationRepository.findAllByOrderByCreatedAtDesc();
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        List<MenuItem> menuItems = menuItemRepository.findAll();

        List<Map<String, Object>> ordersData = new ArrayList<>();
        for (Order o : orders) {
            List<Map<String, Object>> itemsList;
            try {
                itemsList = objectMapper.readValue(o.getItems(), new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception e) {
                itemsList = new ArrayList<>();
            }
            Map<String, Object> map = new HashMap<>();
            map.put("order", o);
            map.put("items_list", itemsList);
            ordersData.add(map);
        }

        model.addAttribute("total_sales", totalSales);
        model.addAttribute("total_bookings", totalBookings);
        model.addAttribute("total_orders", totalOrders);
        model.addAttribute("reservations", reservations);
        model.addAttribute("orders_data", ordersData);
        model.addAttribute("menu_items", menuItems);

        return "admin";
    }

    @PostMapping("/admin/menu/add")
    public String addMenuItem(
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("price") Double price,
            @RequestParam("category") String category,
            @RequestParam("image_url") String imageUrl,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            imageUrl = "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?q=80&w=300&auto=format&fit=crop"; // generic coffee placeholder
        }

        MenuItem item = new MenuItem();
        item.setName(name.trim());
        item.setDescription(description.trim());
        item.setPrice(price);
        item.setCategory(category);
        item.setImageUrl(imageUrl.trim());
        item.setIsAvailable(true);

        menuItemRepository.save(item);

        redirectAttributes.addFlashAttribute("flashMessage", "Menu item added successfully!");
        redirectAttributes.addFlashAttribute("flashCategory", "success");
        return "redirect:/admin";
    }

    @PostMapping("/admin/menu/delete/{itemId}")
    public String deleteMenuItem(
            @PathVariable("itemId") Long itemId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        Optional<MenuItem> itemOpt = menuItemRepository.findById(itemId);
        if (itemOpt.isPresent()) {
            menuItemRepository.delete(itemOpt.get());
            redirectAttributes.addFlashAttribute("flashMessage", "Menu item deleted.");
            redirectAttributes.addFlashAttribute("flashCategory", "success");
        } else {
            redirectAttributes.addFlashAttribute("flashMessage", "Menu item not found.");
            redirectAttributes.addFlashAttribute("flashCategory", "error");
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/order/status/{orderId}")
    public String updateOrderStatus(
            @PathVariable("orderId") Long orderId,
            @RequestParam("status") String status,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isPresent() && (Arrays.asList("Preparing", "Completed").contains(status))) {
            Order order = orderOpt.get();
            order.setOrderStatus(status);
            orderRepository.save(order);
            redirectAttributes.addFlashAttribute("flashMessage", "Order #" + order.getId() + " status updated to " + status + ".");
            redirectAttributes.addFlashAttribute("flashCategory", "success");
        } else {
            redirectAttributes.addFlashAttribute("flashMessage", "Invalid status update.");
            redirectAttributes.addFlashAttribute("flashCategory", "error");
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/location/add")
    public String addLocation(
            @RequestParam("name") String name,
            @RequestParam("address") String address,
            @RequestParam("map_url") String mapUrl,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam(value = "hourly_rate", required = false) String hourlyRateStr,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        if (name == null || name.trim().isEmpty() ||
            address == null || address.trim().isEmpty() ||
            mapUrl == null || mapUrl.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("flashMessage", "Name, Address and Navigation/Map URL are required.");
            redirectAttributes.addFlashAttribute("flashCategory", "error");
            return "redirect:/admin";
        }

        double hourlyRate = 100.0;
        if (hourlyRateStr != null && !hourlyRateStr.trim().isEmpty()) {
            try {
                hourlyRate = Double.parseDouble(hourlyRateStr.trim());
            } catch (NumberFormatException e) {
                hourlyRate = 100.0;
            }
        }

        CafeLocation loc = new CafeLocation();
        loc.setName(name.trim());
        loc.setAddress(address.trim());
        loc.setMapUrl(mapUrl.trim());
        loc.setPhone(phone != null ? phone.trim() : null);
        loc.setHourlyRate(hourlyRate);

        locationRepository.save(loc);

        redirectAttributes.addFlashAttribute("flashMessage", "Cafe location added successfully!");
        redirectAttributes.addFlashAttribute("flashCategory", "success");
        return "redirect:/admin";
    }

    @PostMapping("/admin/location/update-rate/{locationId}")
    public String updateLocationRate(
            @PathVariable("locationId") Long locationId,
            @RequestParam("hourly_rate") String hourlyRateStr,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        Optional<CafeLocation> locOpt = locationRepository.findById(locationId);
        if (locOpt.isPresent()) {
            CafeLocation loc = locOpt.get();
            try {
                double hourlyRate = Double.parseDouble(hourlyRateStr.trim());
                loc.setHourlyRate(hourlyRate);
                locationRepository.save(loc);
                redirectAttributes.addFlashAttribute("flashMessage", "Hourly rate for " + loc.getName() + " updated successfully!");
                redirectAttributes.addFlashAttribute("flashCategory", "success");
            } catch (NumberFormatException e) {
                redirectAttributes.addFlashAttribute("flashMessage", "Invalid hourly rate entered.");
                redirectAttributes.addFlashAttribute("flashCategory", "error");
            }
        } else {
            redirectAttributes.addFlashAttribute("flashMessage", "Location not found.");
            redirectAttributes.addFlashAttribute("flashCategory", "error");
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/reservation/update-charge/{resId}")
    public String updateReservationCharge(
            @PathVariable("resId") Long resId,
            @RequestParam("total_charge") String chargeStr,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        Optional<Reservation> resOpt = reservationRepository.findById(resId);
        if (resOpt.isPresent()) {
            Reservation res = resOpt.get();
            try {
                double charge = Double.parseDouble(chargeStr.trim());
                res.setTotalCharge(charge);
                reservationRepository.save(res);
                redirectAttributes.addFlashAttribute("flashMessage", "Booking charge for KC-RES-" + res.getId() + " updated successfully!");
                redirectAttributes.addFlashAttribute("flashCategory", "success");
            } catch (NumberFormatException e) {
                redirectAttributes.addFlashAttribute("flashMessage", "Invalid charge value entered.");
                redirectAttributes.addFlashAttribute("flashCategory", "error");
            }
        } else {
            redirectAttributes.addFlashAttribute("flashMessage", "Reservation not found.");
            redirectAttributes.addFlashAttribute("flashCategory", "error");
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/location/delete/{locationId}")
    public String deleteLocation(
            @PathVariable("locationId") Long locationId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        Optional<CafeLocation> locOpt = locationRepository.findById(locationId);
        if (locOpt.isPresent()) {
            locationRepository.delete(locOpt.get());
            redirectAttributes.addFlashAttribute("flashMessage", "Cafe location deleted successfully.");
            redirectAttributes.addFlashAttribute("flashCategory", "success");
        } else {
            redirectAttributes.addFlashAttribute("flashMessage", "Location not found.");
            redirectAttributes.addFlashAttribute("flashCategory", "error");
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/menu/update-price/{itemId}")
    public String updateMenuPrice(
            @PathVariable("itemId") Long itemId,
            @RequestParam("price") Double price,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        Optional<MenuItem> itemOpt = menuItemRepository.findById(itemId);
        if (itemOpt.isPresent()) {
            MenuItem item = itemOpt.get();
            item.setPrice(price);
            menuItemRepository.save(item);
            redirectAttributes.addFlashAttribute("flashMessage", "Price for " + item.getName() + " updated successfully!");
            redirectAttributes.addFlashAttribute("flashCategory", "success");
        } else {
            redirectAttributes.addFlashAttribute("flashMessage", "Item not found.");
            redirectAttributes.addFlashAttribute("flashCategory", "error");
        }
        return "redirect:/admin";
    }
}
