package sg.edu.nus.iss.edgp.admin.management.strategy.impl;


import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import sg.edu.nus.iss.edgp.admin.management.aws.service.SQSPublishingService;
import sg.edu.nus.iss.edgp.admin.management.configuration.JWTConfig;
import sg.edu.nus.iss.edgp.admin.management.dto.AuditDTO;
import sg.edu.nus.iss.edgp.admin.management.enums.AuditResponseStatus;
import sg.edu.nus.iss.edgp.admin.management.enums.HTTPVerb;
import sg.edu.nus.iss.edgp.admin.management.jwt.JWTService;
import sg.edu.nus.iss.edgp.admin.management.service.IAuditService;

@AllArgsConstructor
@Service
public class AuditService implements IAuditService {

	private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
	
	private final SQSPublishingService sqsPublishingService;
	
	private final JWTService jwtService;

	@Override
	public void sendMessage(AuditDTO autAuditDTO, String authorizationHeader) {

		try {
			String jwtToken ="";
			String userName = "Invalid Username";
			if(authorizationHeader.length()>0) {
			 jwtToken = authorizationHeader.substring(7);
			}
			

			if (!jwtToken.isEmpty()) {
			   userName = Optional.ofNullable(jwtService.retrieveUserName(jwtToken))
		                   .orElse("Invalid Username");
			   autAuditDTO.setUsername(userName);

			}
			autAuditDTO.setUsername(userName);
			sqsPublishingService.sendMessage(autAuditDTO);

		} catch (Exception e) {
			
			logger.error("Error sending generateMessage to SQS: {}", e);
		}

	}
	
	public  AuditDTO createAuditDTO(String userId, String activityType,String activityTypePrefix, String endpoint, HTTPVerb verb) {
	    AuditDTO auditDTO = new AuditDTO();
	    auditDTO.setActivityType(activityTypePrefix.trim() + activityType);
	    auditDTO.setUserId(userId);
	    auditDTO.setRequestType(verb);
	    auditDTO.setRequestActionEndpoint(endpoint);
	    return auditDTO;
	}
	
	public void logAudit(AuditDTO auditDTO,int stausCode, String message, String authorizationHeader) {
	    auditDTO.setStatusCode(stausCode);
	    if (stausCode ==200) {
	    	  auditDTO.setResponseStatus(AuditResponseStatus.SUCCESS);
	  
	    }else {
	    	  auditDTO.setResponseStatus(AuditResponseStatus.FAILED);
	    }
	    auditDTO.setActivityDescription(message);
	    this.sendMessage(auditDTO, authorizationHeader);
	    
	}


}


