package com.propertyMicroservice.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.propertyMicroservice.constants.AppConstants;
import com.propertyMicroservice.dto.APIResponse;
import com.propertyMicroservice.dto.EmailRequest;
import com.propertyMicroservice.dto.PropertyDto;
import com.propertyMicroservice.dto.RoomsDto;
import com.propertyMicroservice.entity.Area;
import com.propertyMicroservice.entity.City;
import com.propertyMicroservice.entity.Property;
import com.propertyMicroservice.entity.PropertyPhotos;
import com.propertyMicroservice.entity.RoomAvailability;
import com.propertyMicroservice.entity.Rooms;
import com.propertyMicroservice.entity.State;
import com.propertyMicroservice.repository.AreaRepository;
import com.propertyMicroservice.repository.CityRepository;
import com.propertyMicroservice.repository.PropertyPhotosRepository;
import com.propertyMicroservice.repository.PropertyRepository;
import com.propertyMicroservice.repository.RoomAvailabilityRepository;
import com.propertyMicroservice.repository.RoomRepository;
import com.propertyMicroservice.repository.StateRepository;


@Service
public class PropertyService {
	  
	@Autowired
    private PropertyRepository propertyRepository;
    @Autowired
    private AreaRepository areaRepository;
    @Autowired
    private CityRepository cityRepository;
    @Autowired
    private StateRepository stateRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private PropertyPhotosRepository propertyPhotosRepository;
    @Autowired
    private RoomAvailabilityRepository availabilityRepository;
    @Autowired
    private S3Service s3Service;
    
    @Autowired
	private KafkaTemplate<String, EmailRequest> kafkaTemplate;
    
	

    public Property addProperty(PropertyDto dto, MultipartFile[] files) {

       String cityName = dto.getCity();
       City city = cityRepository.findByName(cityName);
       
       String areaName = dto.getArea();
       Area area = areaRepository.findByName(areaName);
       
       String stateName = dto.getState();
       State state = stateRepository.findByName(stateName);
       
       Property property = new Property();
	    property.setName(dto.getName());
	    property.setNumberOfBathrooms(dto.getNumberOfBathrooms());
	    property.setNumberOfBeds(dto.getNumberOfBeds());
	    property.setNumberOfRooms(dto.getNumberOfRooms());
	    property.setNumberOfGuestAllowed(dto.getNumberOfGuestAllowed());
	    property.setArea(area);
	    property.setCity(city);
	    property.setState(state);
	    
	    Property savedProperty = propertyRepository.save(property);
	    
       

        for (RoomsDto roomsDto : dto.getRooms()) {
            Rooms rooms = new Rooms();
            rooms.setProperty(savedProperty);
            rooms.setRoomType(roomsDto.getRoomType());
            rooms.setBasePrice(roomsDto.getBasePrice());
                  
            roomRepository.save(rooms);
        }
        
     // sends msg to kafka topic
        
        EmailRequest emailRequest = new EmailRequest("ajaysahni854302@gmail.com","Property Added","Your property details are not live");
     		kafkaTemplate.send(AppConstants.TOPIC, emailRequest);
        
       

			
			  // upload files to s3 
     		
     		List<String> fileUrls = s3Service.uploadFiles(files);
			  
			  for (String url :fileUrls) {
				  PropertyPhotos photo = new PropertyPhotos();
			      photo.setUrl(url); 
			      photo.setProperty(savedProperty);
			      propertyPhotosRepository.save(photo); 
			  }
			 
     
        return savedProperty;
    }

    // Search property 

    public APIResponse searchProperty(String city, LocalDate date) {
		List<Property> properties = propertyRepository.searchProperty(city,date);
		APIResponse<List<Property>> response = new APIResponse<>();
		
		response.setMessage("Search result");
		response.setStatus(200);
		response.setData(properties);
		
		return response;
	}
    
	
    // search-property by id
    
	public APIResponse<PropertyDto> findPropertyById(long id){
		
		APIResponse<PropertyDto> response = new APIResponse<>();
		
		PropertyDto dto  = new PropertyDto();
		
		Optional<Property> opProp = propertyRepository.findById(id);
		
		if(opProp.isPresent()) {
			Property property = opProp.get();
			dto.setArea(property.getArea().getName());
			dto.setCity(property.getCity().getName());
			dto.setState(property.getState().getName());
			List<Rooms> rooms = property.getRooms();
			
			List<RoomsDto> roomsDto = new ArrayList<>();
			
			for(Rooms room:rooms) {
				RoomsDto roomDto = new RoomsDto();
				BeanUtils.copyProperties(room, roomDto);
				roomsDto.add(roomDto);
			}
			
			dto.setRooms(roomsDto);
			BeanUtils.copyProperties(property, dto);
			response.setMessage("Matching Record");
			response.setStatus(200);
			response.setData(dto);
			return response;
		}
		
		return null;
	}

	
	  public List<RoomAvailability> getTotalRoomsAvailable(long id) { return
	  availabilityRepository.findByRoomId(id);
	  
	  }
	  
	  public Rooms getRoomById(long id) {
		  return roomRepository.findById(id).get();
	  }
	 
      
 }
    
