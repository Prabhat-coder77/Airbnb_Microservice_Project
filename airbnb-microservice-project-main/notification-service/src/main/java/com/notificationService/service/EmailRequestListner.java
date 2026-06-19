package com.notificationService.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notificationService.appconstants.AppConstants;
import com.notificationService.dto.EmailRequest;

@Service
public class EmailRequestListner { 
	
	@Autowired
	private JavaMailSender javaMailSender;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = AppConstants.TOPIC, groupId = "group_email")
    public void kafkaSubscriberContent(String emailRequest) {
        try {
            EmailRequest emailContent = objectMapper.readValue(emailRequest, EmailRequest.class);
            SimpleMailMessage sm = new SimpleMailMessage();
            sm.setTo(emailContent.getTo());
            sm.setSubject(emailContent.getSubject());
            sm.setText(emailContent.getBody());
            javaMailSender.send(sm);
            

        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
