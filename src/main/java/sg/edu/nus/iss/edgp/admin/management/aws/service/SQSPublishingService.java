package sg.edu.nus.iss.edgp.admin.management.aws.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

import sg.edu.nus.iss.edgp.admin.management.dto.AuditDTO;

import java.nio.charset.StandardCharsets;

@Service
public class SQSPublishingService {

	@Value("${aws.sqs.queue.audit.url}")
	String auditQueueURL;
	 
	@Autowired
	private AmazonSQS amazonSQS;
	
	private static final Logger logger = LoggerFactory.getLogger(SQSPublishingService.class);
	
	public void sendMessage(AuditDTO auditDTO) {
	    try {
	    	ObjectMapper objectMapper = new ObjectMapper();

	    	    	
	        String messageBody = objectMapper.writeValueAsString(auditDTO);
	        byte[] messageBytes = messageBody.getBytes(StandardCharsets.UTF_8);
	        int messageSize = messageBytes.length;
	        int maxMessageSize = 256 * 1024; ; // Max Size 256 KB in bytes
	        logger.info("Serialized Audit Log JSON");

	        if (messageSize > maxMessageSize) {
	            logger.warn("Message size exceeds the 256 KB limit: {} bytes, truncating remarks.", messageSize);

	            
	            String truncatedRemarks = truncateMessage(auditDTO.getRemarks(), maxMessageSize, messageBody);
	            auditDTO.setRemarks(truncatedRemarks.concat("..."));

	            messageBody = objectMapper.writeValueAsString(auditDTO);
	            messageBytes = messageBody.getBytes(StandardCharsets.UTF_8);

	            logger.info("Truncated message size: {} bytes", messageBytes.length);
	        }

	        SendMessageRequest sendMsgRequest = new SendMessageRequest()
	                .withQueueUrl(auditQueueURL)
	                .withMessageBody(messageBody)
	                .withDelaySeconds(5);

	        amazonSQS.sendMessage(sendMsgRequest);
	        logger.info("Message sent to SQS");
	    } catch (Exception e) {
	        logger.error("Error sending message to SQS: {}", e);
	    }
	}

	public String truncateMessage(String remarks, int maxMessageSize, String currentMessage) {
	    try {
	       
	        byte[] currentMessageBytes = currentMessage.getBytes(StandardCharsets.UTF_8);
	        int currentSize = currentMessageBytes.length;
	        
	        byte[] remarkBytes = remarks.getBytes(StandardCharsets.UTF_8);
	        
	        int remarkSize =remarkBytes.length;

	        int diffMsgSize = currentSize - maxMessageSize;

	        if (diffMsgSize >= remarkSize) {
	            return "";
	        }	      
	        
	        int  allowedBytesForRemarks = remarkSize - (diffMsgSize+5);
	        if (remarkBytes.length <= allowedBytesForRemarks) {
	            return remarks; 
	        }

	        String truncatedRemarks = new String(remarkBytes, 0, allowedBytesForRemarks, StandardCharsets.UTF_8);
	        return truncatedRemarks;
	    } catch (Exception e) {
	        logger.error("Error while truncating message remarks: {}", e.getMessage());
	        return remarks; 
	    }
	}


}
