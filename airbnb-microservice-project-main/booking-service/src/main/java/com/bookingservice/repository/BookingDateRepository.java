package com.bookingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bookingservice.entity.BookingDate;

@Repository
public interface BookingDateRepository extends JpaRepository<BookingDate, Long> {

}
