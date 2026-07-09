package com.kushwahacafe.cafe.controller;

import com.kushwahacafe.cafe.model.CafeLocation;
import com.kushwahacafe.cafe.model.Reservation;
import com.kushwahacafe.cafe.model.User;
import com.kushwahacafe.cafe.repository.CafeLocationRepository;
import com.kushwahacafe.cafe.repository.ReservationRepository;
import com.kushwahacafe.cafe.repository.UserRepository;
import com.kushwahacafe.cafe.service.EmailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Optional;

@Controller
public class ReservationController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CafeLocationRepository locationRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private ProfileController profileController; // Re-use releaseExpiredReservations on load

    @GetMapping("/reserve")
    public String reservePage(HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("user_id") == null) {
            redirectAttributes.addFlashAttribute("flashMessage", "Please login or register to book a table.");
            redirectAttributes.addFlashAttribute("flashCategory", "error");
            return "redirect:/login";
        }

        profileController.releaseExpiredReservations();
        return "reserve";
    }

    @PostMapping("/reserve")
    public String bookTable(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("phone") String phone,
            @RequestParam("date") String date,
            @RequestParam("slot_in") String slotIn,
            @RequestParam("slot_out") String slotOut,
            @RequestParam("guests") Integer guests,
            @RequestParam(value = "special_requests", required = false) String specialRequests,
            @RequestParam("location_id") Long locationId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long userId = (Long) session.getAttribute("user_id");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("flashMessage", "Please login or register to book a table.");
            redirectAttributes.addFlashAttribute("flashCategory", "error");
            return "redirect:/login";
        }

        profileController.releaseExpiredReservations();

        if (name == null || name.trim().isEmpty() ||
            email == null || email.trim().isEmpty() ||
            phone == null || phone.trim().isEmpty() ||
            date == null || date.trim().isEmpty() ||
            slotIn == null || slotIn.trim().isEmpty() ||
            slotOut == null || slotOut.trim().isEmpty() ||
            guests == null || locationId == null) {
            redirectAttributes.addFlashAttribute("flashMessage", "All fields are required.");
            redirectAttributes.addFlashAttribute("flashCategory", "error");
            return "redirect:/reserve";
        }

        if (slotIn.compareTo(slotOut) >= 0) {
            redirectAttributes.addFlashAttribute("flashMessage", "Slot Out time must be after Slot In time.");
            redirectAttributes.addFlashAttribute("flashCategory", "error");
            return "redirect:/reserve";
        }

        Optional<CafeLocation> locOpt = locationRepository.findById(locationId);
        if (locOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("flashMessage", "Invalid branch/location selected.");
            redirectAttributes.addFlashAttribute("flashCategory", "error");
            return "redirect:/reserve";
        }
        CafeLocation location = locOpt.get();

        double durationHours = 0.0;
        try {
            LocalTime timeIn = LocalTime.parse(slotIn);
            LocalTime timeOut = LocalTime.parse(slotOut);
            long durationSeconds = Duration.between(timeIn, timeOut).toSeconds();
            durationHours = Math.max(0.0, durationSeconds / 3600.0);
        } catch (Exception e) {
            System.err.println("[RESERVATION TIME ERROR] " + e.getMessage());
        }

        if (durationHours < 1.0) {
            redirectAttributes.addFlashAttribute("flashMessage", "You cannot book less than 1 hours.");
            redirectAttributes.addFlashAttribute("flashCategory", "error");
            return "redirect:/reserve";
        }

        double hourlyRate = location.getHourlyRate();
        double totalCharge = Math.round(hourlyRate * durationHours * 100.0) / 100.0;

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }
        User user = userOpt.get();

        Reservation newRes = new Reservation();
        newRes.setUser(user);
        newRes.setName(name.trim());
        newRes.setEmail(email.trim().toLowerCase());
        newRes.setPhone(phone.trim());
        newRes.setDate(date);
        newRes.setSlotIn(slotIn);
        newRes.setSlotOut(slotOut);
        newRes.setStatus("Active");
        newRes.setGuests(guests);
        newRes.setSpecialRequests(specialRequests != null ? specialRequests.trim() : "");
        newRes.setLocation(location);
        newRes.setHourlyRate(hourlyRate);
        newRes.setTotalCharge(totalCharge);

        reservationRepository.save(newRes);

        // Send Email Confirmation to customer
        String customerEmailHtml = String.format(
            "<div style=\"font-family: Arial, sans-serif; border: 1px solid #e2e8f0; padding: 20px; border-radius: 8px; max-width: 600px; background-color: #FCF8F2;\">" +
            "    <h2 style=\"color: #2A1A10; border-bottom: 2px solid #D4AF37; padding-bottom: 10px;\">Kushwaha Cafe Table Booking Confirmation</h2>" +
            "    <p>Dear <b>%s</b>,</p>" +
            "    <p>Your table booking request has been successfully confirmed. Below are your reservation details:</p>" +
            "    <table style=\"width: 100%%; border-collapse: collapse; margin: 15px 0;\">" +
            "        <tr style=\"background-color: #f7fafc;\"><td style=\"padding: 8px; font-weight: bold; border: 1px solid #e2e8f0;\">Reservation ID</td><td style=\"padding: 8px; border: 1px solid #e2e8f0;\">KC-RES-%d</td></tr>" +
            "        <tr><td style=\"padding: 8px; font-weight: bold; border: 1px solid #e2e8f0;\">Branch / Location</td><td style=\"padding: 8px; border: 1px solid #e2e8f0;\">%s</td></tr>" +
            "        <tr style=\"background-color: #f7fafc;\"><td style=\"padding: 8px; font-weight: bold; border: 1px solid #e2e8f0;\">Date</td><td style=\"padding: 8px; border: 1px solid #e2e8f0;\">%s</td></tr>" +
            "        <tr><td style=\"padding: 8px; font-weight: bold; border: 1px solid #e2e8f0;\">Slot In Time</td><td style=\"padding: 8px; border: 1px solid #e2e8f0;\">%s</td></tr>" +
            "        <tr style=\"background-color: #f7fafc;\"><td style=\"padding: 8px; font-weight: bold; border: 1px solid #e2e8f0;\">Slot Out Time</td><td style=\"padding: 8px; border: 1px solid #e2e8f0;\">%s</td></tr>" +
            "        <tr><td style=\"padding: 8px; font-weight: bold; border: 1px solid #e2e8f0;\">Duration</td><td style=\"padding: 8px; border: 1px solid #e2e8f0;\">%.1f Hours</td></tr>" +
            "        <tr style=\"background-color: #f7fafc;\"><td style=\"padding: 8px; font-weight: bold; border: 1px solid #e2e8f0;\">Hourly Rate</td><td style=\"padding: 8px; border: 1px solid #e2e8f0;\">INR %.2f / hr</td></tr>" +
            "        <tr style=\"font-weight: bold; font-size: 1.1em; color: #2A1A10; background-color: #FFF5E6;\"><td style=\"padding: 10px; border: 1px solid #e2e8f0;\">Total Table Charge</td><td style=\"padding: 10px; border: 1px solid #e2e8f0;\">INR %.2f</td></tr>" +
            "        <tr style=\"background-color: #f7fafc;\"><td style=\"padding: 8px; font-weight: bold; border: 1px solid #e2e8f0;\">Number of Guests</td><td style=\"padding: 8px; border: 1px solid #e2e8f0;\">%d Persons</td></tr>" +
            "        <tr><td style=\"padding: 8px; font-weight: bold; border: 1px solid #e2e8f0;\">Contact Phone</td><td style=\"padding: 8px; border: 1px solid #e2e8f0;\">%s</td></tr>" +
            "    </table>" +
            "    %s" +
            "    <p>If you need to make changes or cancel, please log in to your profile or contact us directly.</p>" +
            "    <p>We look forward to serving you!</p>" +
            "    <br>" +
            "    <p>Warm Regards,</p>" +
            "    <p><b>Kushwaha Cafe Team</b></p>" +
            "</div>",
            name, newRes.getId(), location.getName(), date, slotIn, slotOut, durationHours, hourlyRate, totalCharge, guests, phone,
            (specialRequests != null && !specialRequests.trim().isEmpty()) ? "<p><b>Special Note:</b> " + specialRequests.trim() + "</p>" : ""
        );
        emailService.sendEmailNotification(email.trim().toLowerCase(), "Table Booking Confirmed - Kushwaha Cafe (ID: KC-RES-" + newRes.getId() + ")", customerEmailHtml);

        // Send Email Confirmation to Owner
        String ownerEmailHtml = String.format(
            "<div style=\"font-family: Arial, sans-serif; border: 1px solid #e2e8f0; padding: 20px; border-radius: 8px; max-width: 600px; background-color: #FCF8F2;\">" +
            "    <h2 style=\"color: #2A1A10; border-bottom: 2px solid #D4AF37; padding-bottom: 10px; text-align: center;\">[ADMIN ALERT] New Table Booking</h2>" +
            "    <p>Hello Owner/Admin,</p>" +
            "    <p>A new table booking has been confirmed. Below are the details:</p>" +
            "    <table style=\"width: 100%%; border-collapse: collapse; margin: 15px 0;\">" +
            "        <tr style=\"background-color: #f7fafc;\"><td style=\"padding: 8px; font-weight: bold; border: 1px solid #e2e8f0;\">Reservation ID</td><td style=\"padding: 8px; border: 1px solid #e2e8f0;\">KC-RES-%d</td></tr>" +
            "        <tr><td style=\"padding: 8px; font-weight: bold; border: 1px solid #e2e8f0;\">Branch / Location</td><td style=\"padding: 8px; border: 1px solid #e2e8f0;\">%s</td></tr>" +
            "        <tr style=\"background-color: #f7fafc;\"><td style=\"padding: 8px; font-weight: bold; border: 1px solid #e2e8f0;\">Date</td><td style=\"padding: 8px; border: 1px solid #e2e8f0;\">%s</td></tr>" +
            "        <tr><td style=\"padding: 8px; font-weight: bold; border: 1px solid #e2e8f0;\">Slot In Time</td><td style=\"padding: 8px; border: 1px solid #e2e8f0;\">%s</td></tr>" +
            "        <tr style=\"background-color: #f7fafc;\"><td style=\"padding: 8px; font-weight: bold; border: 1px solid #e2e8f0;\">Slot Out Time</td><td style=\"padding: 8px; border: 1px solid #e2e8f0;\">%s</td></tr>" +
            "        <tr><td style=\"padding: 8px; font-weight: bold; border: 1px solid #e2e8f0;\">Duration</td><td style=\"padding: 8px; border: 1px solid #e2e8f0;\">%.1f Hours</td></tr>" +
            "        <tr style=\"background-color: #f7fafc;\"><td style=\"padding: 8px; font-weight: bold; border: 1px solid #e2e8f0;\">Hourly Rate</td><td style=\"padding: 8px; border: 1px solid #e2e8f0;\">INR %.2f / hr</td></tr>" +
            "        <tr style=\"font-weight: bold; font-size: 1.1em; color: #2A1A10; background-color: #FFF5E6;\"><td style=\"padding: 10px; border: 1px solid #e2e8f0;\">Total Table Charge</td><td style=\"padding: 10px; border: 1px solid #e2e8f0;\">INR %.2f</td></tr>" +
            "        <tr style=\"background-color: #f7fafc;\"><td style=\"padding: 8px; font-weight: bold; border: 1px solid #e2e8f0;\">Customer Name</td><td style=\"padding: 8px; border: 1px solid #e2e8f0;\">%s</td></tr>" +
            "        <tr><td style=\"padding: 8px; font-weight: bold; border: 1px solid #e2e8f0;\">Customer Email</td><td style=\"padding: 8px; border: 1px solid #e2e8f0;\">%s</td></tr>" +
            "        <tr style=\"background-color: #f7fafc;\"><td style=\"padding: 8px; font-weight: bold; border: 1px solid #e2e8f0;\">Customer Phone</td><td style=\"padding: 8px; border: 1px solid #e2e8f0;\">%s</td></tr>" +
            "    </table>" +
            "    <p>Go to <a href=\"http://127.0.0.1:8080/admin\">Admin Dashboard</a> to manage reservations.</p>" +
            "</div>",
            newRes.getId(), location.getName(), date, slotIn, slotOut, durationHours, hourlyRate, totalCharge, name, email, phone
        );
        emailService.sendEmailNotification("parkease0563@gmail.com", "[NEW RESERVATION ALERT] Table Booked (ID: KC-RES-" + newRes.getId() + ")", ownerEmailHtml);

        redirectAttributes.addFlashAttribute("flashMessage", "Table reservation confirmed! Check your email for details.");
        redirectAttributes.addFlashAttribute("flashCategory", "success");
        return "redirect:/profile";
    }
}
