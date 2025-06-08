package sg.edu.nus.iss.edgp.admin.management.service;

import sg.edu.nus.iss.edgp.admin.management.dto.UserInvitationDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.UserRequest;
import sg.edu.nus.iss.edgp.admin.management.entity.UserInvitation;

public interface IUserInvitationService {
	
	UserInvitationDTO createInvitation (UserRequest userReq,String authorizationHeader);
	
	UserInvitationDTO updateInvitation (UserRequest userReq);
	
	String generateSecureToken() ;
	
	boolean existsByEmailIsUsed(String email);
	
	UserInvitation findByTokenAndEmail(String token,String email);
	
	
	
}
