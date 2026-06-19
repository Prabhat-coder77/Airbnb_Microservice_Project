package com.propertyMicroservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.propertyMicroservice.entity.Rooms;

@Repository
public interface RoomRepository extends JpaRepository<Rooms, Long> {

}
