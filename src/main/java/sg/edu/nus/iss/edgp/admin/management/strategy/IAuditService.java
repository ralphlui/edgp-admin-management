package sg.edu.nus.iss.edgp.admin.management.strategy;

import sg.edu.nus.iss.edgp.admin.management.dto.AuditDTO;

public interface IAuditService {
	void sendMessage(AuditDTO autAuditDTO, String authorizationHeader);
	
	void sendAuditLogToSqs(String statusCode, String userId, String username, String activityType, String activityDescription,
			String requestActionEndpoint, String responseStatus, String requestType, String remarks);
 
}