package sg.edu.nus.iss.edgp.admin.management.service;

import sg.edu.nus.iss.edgp.admin.management.dto.UserInvitationDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.UserRequest;

public interface IUserInvitationService {
	
	UserInvitationDTO createInvitation (UserRequest userReq);
	
	String generateSecureToken() ;
	
	boolean existsByEmailIsUsed(String email);
	
}
