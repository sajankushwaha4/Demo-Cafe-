package com.kushwahacafe.cafe.config;

import com.kushwahacafe.cafe.model.CafeLocation;
import com.kushwahacafe.cafe.model.MenuItem;
import com.kushwahacafe.cafe.model.User;
import com.kushwahacafe.cafe.repository.CafeLocationRepository;
import com.kushwahacafe.cafe.repository.MenuItemRepository;
import com.kushwahacafe.cafe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MenuItemRepository menuItemRepository;

    @Autowired
    private CafeLocationRepository cafeLocationRepository;

    @Override
    public void run(String... args) throws Exception {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        // 1. Seed Admin User
        java.util.Optional<User> adminOpt = userRepository.findByUsername("admin");
        if (adminOpt.isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("avsktechnologies@gmail.com");
            admin.setPasswordHash(encoder.encode("adminpassword"));
            admin.setRole("admin");
            userRepository.save(admin);
            System.out.println("Admin user created (Username: admin, Password: adminpassword, Email: avsktechnologies@gmail.com).");
        } else {
            User admin = adminOpt.get();
            if (!"avsktechnologies@gmail.com".equalsIgnoreCase(admin.getEmail())) {
                admin.setEmail("avsktechnologies@gmail.com");
                userRepository.save(admin);
                System.out.println("Admin email updated to avsktechnologies@gmail.com.");
            } else {
                System.out.println("Admin user already exists with correct email.");
            }
        }

        // 2. Seed Menu Items
        if (menuItemRepository.count() == 0) {
            List<MenuItem> initialItems = Arrays.asList(
                // Hot Drinks
                createMenuItem("Classic Espresso", "Rich and bold double shot of our house espresso blend.", 90.00, "Hot Drinks", "/static/images/classic_espresso.png"),
                createMenuItem("Cappuccino Premium", "Espresso balanced with steamed milk and a thick layer of creamy foam, dusted with cocoa.", 140.00, "Hot Drinks", "https://images.unsplash.com/photo-1572442388796-11668a67e53d?q=80&w=300&auto=format&fit=crop"),
                createMenuItem("Vanilla Cafe Latte", "Double shot of espresso, steamed milk, and sweet Madagascar vanilla syrup.", 160.00, "Hot Drinks", "https://images.unsplash.com/photo-1541167760496-1628856ab772?q=80&w=300&auto=format&fit=crop"),
                createMenuItem("Masala Chai Tea", "Traditional Indian black tea brewed with fresh ginger, cardamom, cloves, and milk.", 80.00, "Hot Drinks", "https://images.unsplash.com/photo-1576092768241-dec231879fc3?q=80&w=300&auto=format&fit=crop"),
                
                // Cold Drinks
                createMenuItem("Iced Caramel Macchiato", "Chilled milk marked with espresso, sweetened with vanilla syrup and topped with buttery caramel drizzle.", 180.00, "Cold Drinks", "https://images.unsplash.com/photo-1461023058943-07fcbe16d735?q=80&w=300&auto=format&fit=crop"),
                createMenuItem("Hazelnut Frappe", "Blended coffee ice drink flavored with roasted hazelnut syrup, finished with whipped cream.", 190.00, "Cold Drinks", "https://images.unsplash.com/photo-1572490122747-3968b75cc699?q=80&w=300&auto=format&fit=crop"),
                createMenuItem("Mint Mojito Cooler", "A refreshing non-alcoholic blend of fresh mint leaves, lime wedges, sugar, and sparkling water.", 120.00, "Cold Drinks", "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?q=80&w=300&auto=format&fit=crop"),
                
                // Snacks
                createMenuItem("Cheese Garlic Bread", "Four slices of toasted baguette topped with house garlic butter, mozzarella, and herbs.", 130.00, "Snacks", "/static/images/cheese_garlic_bread.png"),
                createMenuItem("Paneer Tikka Sandwich", "Spiced paneer cubes, capsicum, and onions in a creamy mint chutney spread, grilled in wheat bread.", 150.00, "Snacks", "https://images.unsplash.com/photo-1528735602780-2552fd46c7af?q=80&w=300&auto=format&fit=crop"),
                
                // Desserts
                createMenuItem("Fudge Brownie with Ice Cream", "Warm, gooey double chocolate brownie served with a scoop of premium vanilla bean ice cream.", 170.00, "Desserts", "https://images.unsplash.com/photo-1606313564200-e75d5e30476c?q=80&w=300&auto=format&fit=crop"),
                createMenuItem("Red Velvet Cheesecake Slice", "A slice of creamy baked cheesecake on a rich cocoa red velvet cake crust.", 210.00, "Desserts", "https://images.unsplash.com/photo-1533134242443-d4fd215305ad?q=80&w=300&auto=format&fit=crop")
            );
            menuItemRepository.saveAll(initialItems);
            System.out.println("Initial menu items seeded.");
        } else {
            System.out.println("Menu items already seeded.");
        }

        // Seed Pizza items if missing
        if (menuItemRepository.findByCategoryAndIsAvailable("Pizza", true).isEmpty()) {
            List<MenuItem> pizzaItems = Arrays.asList(
                createMenuItem("Classic Margherita Pizza", "Classic Italian pizza topped with fresh tomato sauce, mozzarella cheese, and fragrant basil leaves.", 190.00, "Pizza", "/static/images/margherita_pizza.png"),
                createMenuItem("Double Cheese Margherita", "Loaded with a double layer of premium liquid cheese and mozzarella on a golden crust.", 240.00, "Pizza", "https://images.unsplash.com/photo-1593560708920-61dd98c46a4e?q=80&w=300&auto=format&fit=crop"),
                createMenuItem("Paneer Tikka Pizza", "Tandoori-spiced paneer cubes, capsicum, sliced onions, and green chilies on a spicy marinara base.", 270.00, "Pizza", "https://images.unsplash.com/photo-1513104890138-7c749659a591?q=80&w=300&auto=format&fit=crop"),
                createMenuItem("Veggie Supreme Pizza", "A rich mix of black olives, sweet corn, bell peppers, button mushrooms, and red onions with fresh herbs.", 290.00, "Pizza", "https://images.unsplash.com/photo-1571407970349-bc81e7e96d47?q=80&w=300&auto=format&fit=crop")
            );
            menuItemRepository.saveAll(pizzaItems);
            System.out.println("Pizza items seeded.");
        }

        // Seed Burger items if missing
        if (menuItemRepository.findByCategoryAndIsAvailable("Burger", true).isEmpty()) {
            List<MenuItem> burgerItems = Arrays.asList(
                createMenuItem("Classic Veggie Burger", "Crispy mixed vegetable patty served with lettuce, sliced tomatoes, creamy mayo, and toasted sesame buns.", 110.00, "Burger", "https://images.unsplash.com/photo-1550547660-d9450f859349?q=80&w=300&auto=format&fit=crop"),
                createMenuItem("Spicy Paneer Burger", "A thick block of golden-fried paneer patty, jalapeños, lettuce, and spicy chipotle sauce.", 150.00, "Burger", "/static/images/spicy_paneer_burger.png"),
                createMenuItem("Cheese Burst Veg Burger", "Crispy potato patty stuffed with liquid cheese, topped with sliced onions and mustard relish.", 170.00, "Burger", "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?q=80&w=300&auto=format&fit=crop"),
                createMenuItem("Double Decker Premium Burger", "Double vegetable patties, double slice of cheddar cheese, caramelized onions, and house special sauce.", 210.00, "Burger", "https://images.unsplash.com/photo-1586190848861-99aa4a171e90?q=80&w=300&auto=format&fit=crop")
            );
            menuItemRepository.saveAll(burgerItems);
            System.out.println("Burger items seeded.");
        }

        // 3. Seed Cafe Locations
        if (cafeLocationRepository.count() == 0) {
            List<CafeLocation> initialLocations = Arrays.asList(
                createLocation("Kushwaha Cafe - Sector 62 Branch", "Sector-62, Noida, Uttar Pradesh, India", "https://www.google.com/maps/search/?api=1&query=Sector-62,+Noida,+Uttar+Pradesh,+India", "+91 98765 43210"),
                createLocation("Kushwaha Cafe - Gorakhpur Branch", "Near Station Road, Gorakhpur, Uttar Pradesh, India", "https://www.google.com/maps/search/?api=1&query=Station+Road,+Gorakhpur,+Uttar+Pradesh,+India", "+91 74084 33563")
            );
            cafeLocationRepository.saveAll(initialLocations);
            System.out.println("Initial cafe locations seeded.");
        } else {
            System.out.println("Cafe locations already seeded.");
        }
    }

    private MenuItem createMenuItem(String name, String description, Double price, String category, String imageUrl) {
        MenuItem item = new MenuItem();
        item.setName(name);
        item.setDescription(description);
        item.setPrice(price);
        item.setCategory(category);
        item.setImageUrl(imageUrl);
        item.setIsAvailable(true);
        return item;
    }

    private CafeLocation createLocation(String name, String address, String mapUrl, String phone) {
        CafeLocation loc = new CafeLocation();
        loc.setName(name);
        loc.setAddress(address);
        loc.setMapUrl(mapUrl);
        loc.setPhone(phone);
        loc.setHourlyRate(100.0);
        return loc;
    }
}
