package sg.edu.nus.iss.edgp.admin.management.service;

import sg.edu.nus.iss.edgp.admin.management.dto.AuditDTO;

public interface IAuditService {
	void sendMessage(AuditDTO autAuditDTO, String authorizationHeader);
 
}