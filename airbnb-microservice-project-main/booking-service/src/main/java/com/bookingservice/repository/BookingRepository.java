package com.bookingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookingservice.entity.Bookings;

@Repository
public interface BookingRepository extends JpaRepository<Bookings, Long> {

}
