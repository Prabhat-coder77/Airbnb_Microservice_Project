package com.propertyMicroservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.propertyMicroservice.entity.Area;

@Repository
public interface AreaRepository extends JpaRepository<Area, Long> {
	
	Area findByName(String name);


}
