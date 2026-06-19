package com.propertyMicroservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.propertyMicroservice.entity.State;

@Repository
public interface StateRepository extends JpaRepository<State, Long> {
	
	State findByName(String name);

}
