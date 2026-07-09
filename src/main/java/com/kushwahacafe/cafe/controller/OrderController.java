package com.kushwahacafe.cafe.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kushwahacafe.cafe.model.MenuItem;
import com.kushwahacafe.cafe.model.Order;
import com.kushwahacafe.cafe.model.User;
import com.kushwahacafe.cafe.repository.MenuItemRepository;
import com.kushwahacafe.cafe.repository.OrderRepository;
import com.kushwahacafe.cafe.repository.UserRepository;
import com.kushwahacafe.cafe.service.EmailService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
public class OrderController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EmailService emailService;

    private final ObjectMapper objectMapper = new ObjectMapper();


    // --- JSON Input classes for REST APIs ---
    public static class CartItem {
        public Long id;
        public String name;
        public Double price;
        public Integer quantity;
    }

    public static class CreateOrderRequest {
        public String customer_name;
        public String customer_phone;
        public List<CartItem> cart;
    }

    public static class PayOrderRequest {
        public String utr_number;
    }


    // --- REST: Create Order ---
    @PostMapping("/api/orders/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestBody CreateOrderRequest request,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        Long userId = (Long) session.getAttribute("user_id");
        if (userId == null) {
            response.put("success", false);
            response.put("message", "Please login to checkout.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        if (request == null || request.cart == null || request.cart.isEmpty() ||
            request.customer_name == null || request.customer_name.trim().isEmpty() ||
            request.customer_phone == null || request.customer_phone.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "Invalid order data.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("message", "User not found.");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        User user = userOpt.get();

        double total = 0.0;
        List<Map<String, Object>> verifiedItems = new ArrayList<>();

        for (CartItem item : request.cart) {
            Optional<MenuItem> dbItemOpt = menuItemRepository.findById(item.id);
            if (dbItemOpt.isEmpty() || !dbItemOpt.get().getIsAvailable()) {
                response.put("success", false);
                response.put("message", "Item '" + item.name + "' is not available.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            MenuItem dbItem = dbItemOpt.get();
            int qty = item.quantity;
            double itemTotal = dbItem.getPrice() * qty;
            total += itemTotal;

            Map<String, Object> verifiedItem = new HashMap<>();
            verifiedItem.put("id", dbItem.getId());
            verifiedItem.put("name", dbItem.getName());
            verifiedItem.put("price", dbItem.getPrice());
            verifiedItem.put("quantity", qty);
            verifiedItem.put("total", itemTotal);
            verifiedItems.add(verifiedItem);
        }

        Order newOrder = new Order();
        newOrder.setUser(user);
        newOrder.setCustomerName(request.customer_name.trim());
        newOrder.setCustomerPhone(request.customer_phone.trim());
        newOrder.setTotalAmount(total);
        newOrder.setPaymentStatus("Pending");
        newOrder.setOrderStatus("Pending");

        try {
            newOrder.setItems(objectMapper.writeValueAsString(verifiedItems));
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to process order items.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        orderRepository.save(newOrder);

        response.put("success", true);
        response.put("order_id", newOrder.getId());
        response.put("total_amount", total);
        return ResponseEntity.ok(response);
    }

    // --- VIEW: Checkout Page ---
    @GetMapping("/checkout")
    public String checkout(
            @RequestParam("order_id") Long orderId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Long userId = (Long) session.getAttribute("user_id");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("flashMessage", "Please login to continue checkout.");
            redirectAttributes.addFlashAttribute("flashCategory", "error");
            return "redirect:/login";
        }

        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty() || !orderOpt.get().getUser().getId().equals(userId)) {
            redirectAttributes.addFlashAttribute("flashMessage", "Order not found.");
            redirectAttributes.addFlashAttribute("flashCategory", "error");
            return "redirect:/menu";
        }

        Order order = orderOpt.get();
        if ("Paid".equals(order.getPaymentStatus())) {
            return "redirect:/bill/" + order.getId();
        }

        String payeeUpi = "7408433563@upi";
        String payeeName = "Kushwaha Cafe";
        String upiString = String.format("upi://pay?pa=%s&pn=%s&am=%.2f&cu=INR",
                payeeUpi, payeeName.replace(" ", "%20"), order.getTotalAmount());

        model.addAttribute("order", order);
        model.addAttribute("upi_string", upiString);
        model.addAttribute("payee_upi", payeeUpi);
        return "checkout";
    }

    // --- REST: Pay Order ---
    @PostMapping("/api/orders/pay/{orderId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> payOrder(
            @PathVariable("orderId") Long orderId,
            @RequestBody PayOrderRequest request,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();
        Long userId = (Long) session.getAttribute("user_id");
        if (userId == null) {
            response.put("success", false);
            response.put("message", "Unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty() || !orderOpt.get().getUser().getId().equals(userId)) {
            response.put("success", false);
            response.put("message", "Order not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        Order order = orderOpt.get();
        if ("Paid".equals(order.getPaymentStatus())) {
            response.put("success", true);
            response.put("message", "Already Paid");
            response.put("order_id", order.getId());
            return ResponseEntity.ok(response);
        }

        String utrNumber = request != null ? request.utr_number : "";
        if (utrNumber == null || utrNumber.trim().isEmpty()) {
            // Auto-generate a 12-digit mock transaction ID if empty
            long randomUtr = 100000000000L + (long)(new java.util.Random().nextDouble() * 900000000000L);
            utrNumber = String.valueOf(randomUtr);
        } else if (utrNumber.trim().length() != 12 || !utrNumber.trim().matches("\\d+")) {
            response.put("success", false);
            response.put("message", "Please enter a valid 12-digit UPI Ref/UTR number.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        utrNumber = utrNumber.trim();
        order.setPaymentStatus("Paid");
        order.setOrderStatus("Preparing");
        order.setTransactionId(utrNumber);
        orderRepository.save(order);

        sendInvoiceAndOwnerEmails(order, userId);

        response.put("success", true);
        response.put("message", "Payment confirmed!");
        response.put("order_id", order.getId());
        return ResponseEntity.ok(response);
    }


    private void sendInvoiceAndOwnerEmails(Order order, Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            List<Map<String, Object>> itemsList;
            try {
                itemsList = objectMapper.readValue(order.getItems(), new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception e) {
                itemsList = new ArrayList<>();
            }

            StringBuilder itemsRows = new StringBuilder();
            for (Map<String, Object> it : itemsList) {
                itemsRows.append(String.format(
                    "<tr>" +
                    "    <td style=\"padding: 8px; border: 1px solid #e2e8f0;\">%s</td>" +
                    "    <td style=\"padding: 8px; border: 1px solid #e2e8f0; text-align: center;\">%d</td>" +
                    "    <td style=\"padding: 8px; border: 1px solid #e2e8f0; text-align: right;\">INR %.2f</td>" +
                    "    <td style=\"padding: 8px; border: 1px solid #e2e8f0; text-align: right;\">INR %.2f</td>" +
                    "</tr>",
                    it.get("name"), it.get("quantity"), Double.valueOf(it.get("price").toString()), Double.valueOf(it.get("total").toString())
                ));
            }

            String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

            String customerEmailHtml = String.format(
                "<div style=\"font-family: Arial, sans-serif; border: 1px solid #e2e8f0; padding: 20px; border-radius: 8px; max-width: 600px; background-color: #FCF8F2;\">" +
                "    <h2 style=\"color: #2A1A10; border-bottom: 2px solid #D4AF37; padding-bottom: 10px; text-align: center;\">Kushwaha Cafe - Order Bill Receipt</h2>" +
                "    <p>Dear <b>%s</b>,</p>" +
                "    <p>Thank you for your order! We have successfully received your payment. Here is your digital receipt:</p>" +
                "    " +
                "    <table style=\"width: 100%%; border-collapse: collapse; margin-bottom: 15px;\">" +
                "        <tr><td><b>Order ID:</b> KC-ORD-%d</td><td style=\"text-align: right;\"><b>Date:</b> %s</td></tr>" +
                "        <tr><td><b>Transaction ID:</b> %s</td><td style=\"text-align: right;\"><b>Status:</b> PAID</td></tr>" +
                "    </table>" +
                "    " +
                "    <table style=\"width: 100%%; border-collapse: collapse; margin: 15px 0;\">" +
                "        <thead>" +
                "            <tr style=\"background-color: #2A1A10; color: white;\">" +
                "                <th style=\"padding: 8px; text-align: left;\">Item</th>" +
                "                <th style=\"padding: 8px; text-align: center;\">Qty</th>" +
                "                <th style=\"padding: 8px; text-align: right;\">Price</th>" +
                "                <th style=\"padding: 8px; text-align: right;\">Total</th>" +
                "            </tr>" +
                "        </thead>" +
                "        <tbody>" +
                "            %s" +
                "            <tr style=\"background-color: #f7fafc; font-weight: bold;\">" +
                "                <td colspan=\"3\" style=\"padding: 8px; text-align: right; border: 1px solid #e2e8f0;\">Grand Total</td>" +
                "                <td style=\"padding: 8px; text-align: right; border: 1px solid #e2e8f0;\">INR %.2f</td>" +
                "            </tr>" +
                "        </tbody>" +
                "    </table>" +
                "    " +
                "    <p style=\"text-align: center; font-style: italic; margin-top: 25px; color: #555;\">We are now preparing your delicious food. Visit us again soon!</p>" +
                "    <br>" +
                "    <p>Warm Regards,</p>" +
                "    <p><b>Kushwaha Cafe Team</b></p>" +
                "</div>",
                order.getCustomerName(), order.getId(), nowStr, order.getTransactionId(), itemsRows, order.getTotalAmount()
            );
            emailService.sendEmailNotification(user.getEmail(), "Invoice: Order KC-ORD-" + order.getId() + " Paid successfully!", customerEmailHtml);

            // Send Email notification to owner (Admin)
            String ownerEmailHtml = String.format(
                "<div style=\"font-family: Arial, sans-serif; border: 1px solid #e2e8f0; padding: 20px; border-radius: 8px; max-width: 600px; background-color: #FCF8F2;\">" +
                "    <h2 style=\"color: #2A1A10; border-bottom: 2px solid #D4AF37; padding-bottom: 10px; text-align: center;\">Kushwaha Cafe - New Order Alert!</h2>" +
                "    <p>Hello <b>Owner/Admin</b>,</p>" +
                "    <p>A new order has been paid and received. Please start preparation:</p>" +
                "    " +
                "    <table style=\"width: 100%%; border-collapse: collapse; margin-bottom: 15px;\">" +
                "        <tr><td><b>Order ID:</b> KC-ORD-%d</td><td style=\"text-align: right;\"><b>Date:</b> %s</td></tr>" +
                "        <tr><td><b>Customer Name:</b> %s</td><td style=\"text-align: right;\"><b>Phone:</b> %s</td></tr>" +
                "        <tr><td><b>Transaction ID:</b> %s</td><td style=\"text-align: right;\"><b>Status:</b> PAID</td></tr>" +
                "    </table>" +
                "    " +
                "    <table style=\"width: 100%%; border-collapse: collapse; margin: 15px 0;\">" +
                "        <thead>" +
                "            <tr style=\"background-color: #2A1A10; color: white;\">" +
                "                <th style=\"padding: 8px; text-align: left;\">Item</th>" +
                "                <th style=\"padding: 8px; text-align: center;\">Qty</th>" +
                "                <th style=\"padding: 8px; text-align: right;\">Price</th>" +
                "                <th style=\"padding: 8px; text-align: right;\">Total</th>" +
                "            </tr>" +
                "        </thead>" +
                "        <tbody>" +
                "            %s" +
                "            <tr style=\"background-color: #f7fafc; font-weight: bold;\">" +
                "                <td colspan=\"3\" style=\"padding: 8px; text-align: right; border: 1px solid #e2e8f0;\">Grand Total</td>" +
                "                <td style=\"padding: 8px; text-align: right; border: 1px solid #e2e8f0;\">INR %.2f</td>" +
                "            </tr>" +
                "        </tbody>" +
                "    </table>" +
                "    <p>Go to your <a href=\"http://127.0.0.1:8080/admin\">Admin Dashboard</a> to update status.</p>" +
                "</div>",
                order.getId(), nowStr, order.getCustomerName(), order.getCustomerPhone(), order.getTransactionId(), itemsRows, order.getTotalAmount()
            );
            emailService.sendEmailNotification("parkease0563@gmail.com", "[NEW ORDER ALERT] Order KC-ORD-" + order.getId() + " Paid!", ownerEmailHtml);
        }

        // Owner console alert
        System.out.println("\n" + "#".repeat(30) + " OWNER ALERTS " + "#".repeat(30));
        System.out.println("NEW PAID ORDER RECEIVED: Order ID KC-ORD-" + order.getId());
        System.out.println("Customer Name: " + order.getCustomerName() + " | Phone: " + order.getCustomerPhone());
        System.out.printf("Total Amount:  INR %.2f | Txn ID: %s\n", order.getTotalAmount(), order.getTransactionId());
        System.out.println("#".repeat(74) + "\n");
    }

    // --- VIEW: Order Bill Receipt ---
    @GetMapping("/bill/{orderId}")
    public String viewBill(
            @PathVariable("orderId") Long orderId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes) {

        Long userId = (Long) session.getAttribute("user_id");
        String role = (String) session.getAttribute("role");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("flashMessage", "Please login to view bills.");
            redirectAttributes.addFlashAttribute("flashCategory", "error");
            return "redirect:/login";
        }

        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty() || (!orderOpt.get().getUser().getId().equals(userId) && !"admin".equals(role))) {
            redirectAttributes.addFlashAttribute("flashMessage", "Bill not found.");
            redirectAttributes.addFlashAttribute("flashCategory", "error");
            return "redirect:/menu";
        }

        Order order = orderOpt.get();
        List<Map<String, Object>> itemsList;
        try {
            itemsList = objectMapper.readValue(order.getItems(), new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            itemsList = new ArrayList<>();
        }

        double subtotal = order.getTotalAmount() / 1.05;
        double gst = order.getTotalAmount() - subtotal;

        model.addAttribute("order", order);
        model.addAttribute("items", itemsList);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("gst", gst);
        return "bill";
    }

    // --- VIEW: Mock Payment Page ---
    @GetMapping("/mock-pay/{orderId}")
    public String mockPay(
            @PathVariable("orderId") Long orderId,
            Model model) {

        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            return "index";
        }
        model.addAttribute("order", orderOpt.get());
        return "mock_pay";
    }

    // --- REST: Check Order Payment Status ---
    @GetMapping("/api/orders/status/{orderId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getOrderStatus(@PathVariable("orderId") Long orderId) {
        Map<String, Object> response = new HashMap<>();
        Optional<Order> orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            response.put("status", "not_found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        response.put("status", orderOpt.get().getPaymentStatus());
        return ResponseEntity.ok(response);
    }
}
