package com.propertyMicroservice.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.propertyMicroservice.dto.APIResponse;
import com.propertyMicroservice.dto.PropertyDto;
import com.propertyMicroservice.entity.Property;
import com.propertyMicroservice.entity.RoomAvailability;
import com.propertyMicroservice.entity.Rooms;
import com.propertyMicroservice.repository.RoomAvailabilityRepository;
import com.propertyMicroservice.service.PropertyService;

@RestController
@RequestMapping("/api/v1/property")
public class PropertyController {

	@Autowired
    private PropertyService propertyService;
	@Autowired
	private RoomAvailabilityRepository roomAvailabilityRepository;

   // private static final Logger logger = LoggerFactory.getLogger(PropertyController.class);

    // ============================
    // Add Property with Images
    // ============================
    @PostMapping(
            value = "/add-property",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<APIResponse> addProperty(
            @RequestParam("property") String propertyJson,
            @RequestParam("files") MultipartFile[] files) {

      //  logger.info("Property JSON received");
      //  logger.info("Files count: {}", (files != null ? files.length : 0));

        ObjectMapper mapper = new ObjectMapper();
        PropertyDto dto =null;

        try {
            dto = mapper.readValue(propertyJson, PropertyDto.class);
        } catch (JsonProcessingException e) {
        	
        	return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        	
        }
         //   logger.error("Invalid JSON format", e);

        //    APIResponse<PropertyDto> error = new APIResponse<>();
         //   error.setMessage("Invalid property JSON format");
         //   error.setStatus(400);
         //   error.setData(null);

          //  return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        	
        
        Property property = propertyService.addProperty(dto, files);
        
        APIResponse<Property> response = new APIResponse<>();
        response.setMessage("Property Added");
        response.setStatus(201);
        response.setData(property);
        
        return new ResponseEntity<>(response,HttpStatus.CREATED);
        
        
    }

    // ============================
    // Search Property
    // ============================
	
	  @GetMapping("/search-property")
	  
	  public APIResponse searchProperty(
	  
	  @RequestParam String name,
	  
	  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
	  
	  ) {
	  
	  APIResponse response = propertyService.searchProperty(name, date);
	  
	  return response;
	  
	  }
	  
	  
	  //search-property by id
	
	  @GetMapping("/property-id")
		public APIResponse<PropertyDto> getPropertyById(@RequestParam long id){
			APIResponse<PropertyDto> response = propertyService.findPropertyById(id);
			return response;
		}
		
		@GetMapping("/room-available-room-id")
		public APIResponse<List<RoomAvailability>> getTotalRoomsAvailable(@RequestParam long id){
			List<RoomAvailability> totalRooms = propertyService.getTotalRoomsAvailable(id);
			
			APIResponse<List<RoomAvailability>> response = new APIResponse<>();
		    response.setMessage("Total rooms");
		    response.setStatus(200);
		    response.setData(totalRooms);
		    return response;
		}
		
		@GetMapping("/room-id")
		public APIResponse<Rooms> getRoomType(@RequestParam long id){
			Rooms room = propertyService.getRoomById(id);
			
			APIResponse<Rooms> response = new APIResponse<>();
		    response.setMessage("Total rooms");
		    response.setStatus(200);
		    response.setData(room);
		    return response;
		}
		
		// Room update 
		@PutMapping("/updateRoomCount")
		public APIResponse<Boolean> updateRoomCount(@RequestParam long id, @RequestParam LocalDate date){
			APIResponse<Boolean> response = new APIResponse<>();
			RoomAvailability roomsAvailable = roomAvailabilityRepository.getRooms(id, date);
			int count = roomsAvailable.getAvailableCount();
			
			if(count>0) {
				roomsAvailable.setAvailableCount(count-1);
				roomAvailabilityRepository.save(roomsAvailable);
				response.setMessage("updated");
				response.setStatus(200);
				response.setData(true);
				return response;
			}else {
				response.setMessage("No Availability");
				response.setStatus(500);
				response.setData(false);
				return response;
				
			}
			
		}

}
