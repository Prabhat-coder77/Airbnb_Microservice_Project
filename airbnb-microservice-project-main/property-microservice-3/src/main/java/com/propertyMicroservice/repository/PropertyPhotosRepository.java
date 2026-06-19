package com.propertyMicroservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.propertyMicroservice.entity.PropertyPhotos;

public interface PropertyPhotosRepository extends JpaRepository<PropertyPhotos , Long> {

}
