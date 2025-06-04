package sg.edu.nus.iss.edgp.admin.management.service.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sg.edu.nus.iss.edgp.admin.management.dto.UserInvitationDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.UserRequest;
import sg.edu.nus.iss.edgp.admin.management.entity.UserInvitation;
import sg.edu.nus.iss.edgp.admin.management.repository.UserInvitationRepository;
import sg.edu.nus.iss.edgp.admin.management.service.IUserInvitationService;
import sg.edu.nus.iss.edgp.admin.management.utility.DTOMapper;

@Service
public class UserInvitationService implements IUserInvitationService{
	
	private static final Logger logger = LoggerFactory.getLogger(UserInvitationService.class);

	@Autowired 
	private UserInvitationRepository userInvitationRepository;
	 	
	@Override
	public String generateSecureToken() {
		 SecureRandom random = new SecureRandom();
	        byte[] bytes = new byte[32];
	        random.nextBytes(bytes);
	        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	@Override
	public UserInvitationDTO createInvitation(UserRequest userReq) {
		try {
			String token = this.generateSecureToken();

	        UserInvitation invitation = new UserInvitation();
	        invitation.setEmail(userReq.getEmail());
	        invitation.setRoleName(userReq.getRole());
	        invitation.setToken(token);
	        invitation.setExpiresAt(LocalDateTime.now().plusHours(24));
	        invitation.setUsed(false);
	        invitation.setInvitedDate(LocalDateTime.now());
	        

	        UserInvitation dbInvitation =userInvitationRepository.save(invitation);
	        if(dbInvitation ==null) {
	        	throw new Exception("User invitation is not successful");
	        }
	        return DTOMapper.toUserInvitationDTO(dbInvitation);
		}catch(Exception e) {
			logger.error("Error occurred while role creating, " + e.toString());

			 
		}
		return null;
		
		
	}

}
