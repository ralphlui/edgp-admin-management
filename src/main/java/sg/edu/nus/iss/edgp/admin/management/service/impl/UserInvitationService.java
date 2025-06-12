package sg.edu.nus.iss.edgp.admin.management.service.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import javax.management.relation.RoleNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sg.edu.nus.iss.edgp.admin.management.dto.UserInvitationDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.UserRequest;
import sg.edu.nus.iss.edgp.admin.management.entity.*;
import sg.edu.nus.iss.edgp.admin.management.exception.UserNotFoundException;
import sg.edu.nus.iss.edgp.admin.management.repository.*;
import sg.edu.nus.iss.edgp.admin.management.service.IUserInvitationService;
import sg.edu.nus.iss.edgp.admin.management.utility.DTOMapper;
import sg.edu.nus.iss.edgp.admin.management.utility.JSONReader;

@RequiredArgsConstructor
@Service
public class UserInvitationService implements IUserInvitationService {

	private static final Logger logger = LoggerFactory.getLogger(UserInvitationService.class);

	
	private final UserInvitationRepository userInvitationRepository;
    private final RoleRepository roleRepository; 
	private final JSONReader jsonReader;
	private final UserRepository userRepository;
	private final UserOrganizationRepository userOrganizationRepository;

	@Override
	public String generateSecureToken() {
		SecureRandom random = new SecureRandom();
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	@Override
	public UserInvitationDTO createInvitation(UserRequest userReq, String authorizationHeader) {
		try {
			String token = this.generateSecureToken();

			UserInvitation invitation = new UserInvitation();
			invitation.setEmail(userReq.getEmail());
			Role role = roleRepository.findByRoleName(userReq.getRole());
			
			if (role== null) {
				throw new RoleNotFoundException("User Invitation info not found.");
			}
			invitation.setRole(role);
			invitation.setToken(token);
			invitation.setOrganizationId(userReq.getOrganizationId());
			invitation.setExpiresAt(LocalDateTime.now().plusHours(72));
			invitation.setUsed(false);
			invitation.setInvitedDate(LocalDateTime.now());

			UserInvitation dbInvitation = userInvitationRepository.save(invitation);
			if (dbInvitation == null) {
				throw new Exception("User invitation is not successful");
			}			
			
			//send email
			jsonReader.sendUserInviteEmail(dbInvitation, authorizationHeader);
			//
			return DTOMapper.toUserInvitationDTO(dbInvitation);
		} catch (Exception e) {
			logger.error("Error occurred while user invitation, " + e.toString());

		}
		return null;

	}

	@Override
	public boolean existsByEmailIsUsed(String email) {
		try {
			return userInvitationRepository.existsByEmailAndUsedIsTrue(email);
		} catch (Exception e) {
			logger.error("Error occurred while role creating, " + e.toString());

		}
		return false;
	}

	@Override
	public UserInvitation findByTokenAndEmail(String token,String email) {
		try {
			return userInvitationRepository.findByTokenAndEmail(token,email);
			

		} catch (Exception e) {

			logger.error("Error occurred while find by token, " + e.toString());
			
		}
		return null;

	}

	@Override
	public UserInvitationDTO updateInvitation(UserRequest userReq) {
           try {
			
        	   Optional<UserInvitation> dbData = userInvitationRepository.findByToken(userReq.getUserInvitationtoken());
			if (!dbData.isPresent()) {
				throw new UserNotFoundException("User Invitation info not found.");
			}
			dbData.get().setUsed(true);
			dbData.get().setUpdatedDate(LocalDateTime.now());
			
			logger.info("Update User Invitation...");
			UserInvitation updateUserInvitation = userInvitationRepository.save(dbData.get());
			logger.info("User Invitation update  is successful");
			return DTOMapper.toUserInvitationDTO(updateUserInvitation);
		} catch (Exception e) {
			logger.error("Error occurred while user invitation updating", e);
			e.printStackTrace();
			throw e;
		}
	}

}
