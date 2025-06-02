package sg.edu.nus.iss.edgp.admin.management.strategy.impl;


import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import sg.edu.nus.iss.edgp.admin.management.aws.service.SQSPublishingService;
import sg.edu.nus.iss.edgp.admin.management.configuration.AWSConfig;
import sg.edu.nus.iss.edgp.admin.management.configuration.JWTConfig;
import sg.edu.nus.iss.edgp.admin.management.dto.AuditDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.AuditLogRequest;
import sg.edu.nus.iss.edgp.admin.management.enums.AuditResponseStatus;
import sg.edu.nus.iss.edgp.admin.management.enums.HTTPVerb;
import sg.edu.nus.iss.edgp.admin.management.jwt.JWTService;
import sg.edu.nus.iss.edgp.admin.management.strategy.IAuditService;

@RequiredArgsConstructor
@Service
public class AuditService implements IAuditService {

	private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
	
	private final SQSPublishingService sqsPublishingService;
	
	private final JWTService jwtService;
	private final AWSConfig awsConfig;
	private final AmazonSQS amazonSQS;
	
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
			
			sqsPublishingService.sendMessage(autAuditDTO);

		} catch (Exception e) {
			
			logger.error("Error sending generateMessage to SQS: {}", e);
		}

	}
	
	@Async
    @Override
	public void sendAuditLogToSqs(String statusCode, String userId, String username, String activityType, String activityDescription,
			String requestActionEndpoint, String responseStatus, String requestType, String remarks) {
		try {
		    String auditLogRequest = createLogEntryRequest(statusCode, userId, username, activityType, activityDescription,
		        requestActionEndpoint, responseStatus, requestType, remarks);
		    
		    String queueUrl = awsConfig.getSQSUrl();

		    SendMessageRequest sendMessageRequest = new SendMessageRequest()
		            .withQueueUrl(queueUrl)
		            .withMessageBody(auditLogRequest);

		    SendMessageResult sendMessageResult = amazonSQS.sendMessage(sendMessageRequest);
		    logger.info("Message response in SQS: {}",sendMessageResult.getMessageId());
		    
		} catch (Exception e) {
		    // Generic exception handling for any other unforeseen errors
		    logger.error("Exception: Unexpected error occurred while sending audit logs to SQS ", e);
		}
	}
	
	private String createLogEntryRequest(String statusCode, String userId, String username, String activityType,
			String activityDescription, String requestActionEndpoint, String responseStatus, String requestType,
			String remarks) {

		ObjectMapper objectMapper = new ObjectMapper();
		AuditLogRequest logRequest = new AuditLogRequest(statusCode, userId, username,
				activityType, activityDescription, requestActionEndpoint, responseStatus, requestType, remarks);
		try {

			String auditLogString = objectMapper.writeValueAsString(logRequest);
			logger.info("Serialized auditLogString JSON");

			byte[] messageBytes = auditLogString.getBytes(StandardCharsets.UTF_8);
			int messageSize = messageBytes.length;
			int maxMessageSize = 256 * 1024;  // Max Size 256 KB in bytes

			if (messageSize > maxMessageSize) {
				logger.warn("Message size exceeds the 256 KB limit: {} bytes, truncating remarks.", messageSize);

				String truncatedRemarks = truncateMessage(logRequest.getRemarks(), maxMessageSize, auditLogString);
				logRequest.setRemarks(truncatedRemarks.concat("..."));

				auditLogString = objectMapper.writeValueAsString(logRequest);
				messageBytes = auditLogString.getBytes(StandardCharsets.UTF_8);

				logger.info("Truncated message size: {} bytes", messageBytes.length);
			}

			return auditLogString;

		} catch (Exception e) {
			logger.error("Exception: Unexpected error occurred while creatin audit log object", e);
			e.printStackTrace();
		}
		return "";

	}
	
	public String truncateMessage(String remarks, int maxMessageSize, String currentMessage) {
	    try {
	        // Start truncating the remarks field only if it exceeds the limit
	        byte[] currentMessageBytes = currentMessage.getBytes(StandardCharsets.UTF_8);
	        int currentSize = currentMessageBytes.length;
	        
	        byte[] remarkBytes = remarks.getBytes(StandardCharsets.UTF_8);
	        
	        int remarkSize =remarkBytes.length;

	        int diffMsgSize = currentSize - maxMessageSize;

	        if (diffMsgSize >= remarkSize) {
	            return ""; // If no space left for remarks, return an empty string
	        }	      
	        
	        int  allowedBytesForRemarks = remarkSize - (diffMsgSize+5);
	        if (remarkBytes.length <= allowedBytesForRemarks) {
	            return remarks; 
	        }

	        return  new String(remarkBytes, 0, allowedBytesForRemarks, StandardCharsets.UTF_8);
	    } catch (Exception e) {
	        logger.error("Error while truncating message remarks: ", e);
	        return remarks; 
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


