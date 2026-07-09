package com.kushwahacafe.cafe.repository;

import com.kushwahacafe.cafe.model.Reservation;
import com.kushwahacafe.cafe.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByUserOrderByCreatedAtDesc(User user);
    List<Reservation> findByStatus(String status);
    List<Reservation> findAllByOrderByCreatedAtDesc();
}
