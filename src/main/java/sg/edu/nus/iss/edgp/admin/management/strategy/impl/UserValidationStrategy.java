package sg.edu.nus.iss.edgp.admin.management.strategy.impl;

import org.json.simple.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.RequiredArgsConstructor;
import sg.edu.nus.iss.edgp.admin.management.dto.RoleDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.UserRequest;
import sg.edu.nus.iss.edgp.admin.management.dto.ValidationResult;
import sg.edu.nus.iss.edgp.admin.management.entity.User;
import sg.edu.nus.iss.edgp.admin.management.enums.AuditLogInvalidUser;
import sg.edu.nus.iss.edgp.admin.management.service.impl.PasswordValidatorService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.RoleService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.UserInvitationService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.UserService;
import sg.edu.nus.iss.edgp.admin.management.strategy.IAPIHelperValidationStrategy;
import sg.edu.nus.iss.edgp.admin.management.utility.JSONReader;

@RequiredArgsConstructor
@Service
public class UserValidationStrategy implements IAPIHelperValidationStrategy <UserRequest>{

	private final UserService userService;
	
	private final UserInvitationService userInvitationService;
	
	private final PasswordValidatorService passwordValidatorService;
	
	private final RoleService roleService;
	private final JSONReader jsonReader;
	

	private String INVALID_USER_ID = AuditLogInvalidUser.INVALID_USER_ID.toString();
	private String INVALID_USER_NAME = AuditLogInvalidUser.INVALID_USER_NAME.toString();
	
	@Override
	public ValidationResult validateCreation(UserRequest userReq,String header) {
		ValidationResult validationResult = new ValidationResult();

		if (userReq.getEmail() == null || userReq.getEmail().isEmpty()) {

			String userName = StringUtils.hasText(userReq.getUsername()) ? userReq.getUsername()
					: INVALID_USER_NAME;
			validationResult.setMessage("Email cannot be empty.");
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			validationResult.setValid(false);
			validationResult.setUserId(INVALID_USER_ID);
			validationResult.setUserName(userName);
			return validationResult;
		}

		User dbUser = userService.findByEmail(userReq.getEmail());
		if (dbUser != null) {
			validationResult.setMessage(userReq.getEmail() + " is existed.");
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			validationResult.setValid(false);
			validationResult.setUserId(dbUser.getUserId());
			validationResult.setUserName(dbUser.getUsername());
			return validationResult;
		}
		String msgPassword = passwordValidatorService.validatePassword(userReq.getPassword());

		if (!msgPassword.equalsIgnoreCase("Valid")) {
			validationResult.setMessage(msgPassword);
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			validationResult.setValid(false);
			validationResult.setUserId(userReq.getUserId());
			validationResult.setUserName(userReq.getUsername());
			return validationResult;
		}
		
		if (userReq.getRole() == null || userReq.getRole().isEmpty()) {
			validationResult.setMessage("Role cannot be empty.");
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			validationResult.setValid(false);
			return validationResult;
		}else {
		
		RoleDTO roleDTO = roleService.findByRoleName(userReq.getRole());

		if (roleDTO == null || roleDTO.getRoleId() == null || roleDTO.getRoleId().isEmpty()) {
			validationResult.setMessage("Invalid role: " + userReq.getRole());
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			validationResult.setValid(false);
			return validationResult;
		}
		}

		validationResult.setValid(true);
		return validationResult;
	}

	@Override
	public ValidationResult validateUpdating(UserRequest user, String header) {
		ValidationResult validationResult = new ValidationResult();

		if (user.getUserId() == null || user.getUserId().isEmpty()) {
			validationResult.setMessage("User ID cannot be empty.");
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			validationResult.setValid(false);
			validationResult.setUserId(INVALID_USER_ID);
			validationResult.setUserName(INVALID_USER_NAME);
			return validationResult;
		}

		ValidationResult validationObjResult = validateObjectByUserId(user,true);
		if (!validationObjResult.isValid()) {
			return validationObjResult;
		}
		
		if (user.getRole() == null || user.getRole().isEmpty()) {
			validationResult.setMessage("Role cannot be empty.");
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			validationResult.setValid(false);
			return validationResult;
		}else {
		
		RoleDTO roleDTO = roleService.findByRoleName(user.getRole());

		if (roleDTO == null || roleDTO.getRoleId() == null || roleDTO.getRoleId().isEmpty()) {
			validationResult.setMessage("Invalid role: " + user.getRole());
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			validationResult.setValid(false);
			return validationResult;
		}
		}
		

		validationResult.setValid(true);
		return validationResult;
	}

	@Override
	public ValidationResult validateObject(String email) {
		User user = userService.findByEmail(email);

		if (user == null) {
			return validateUserNotFound(INVALID_USER_ID, INVALID_USER_NAME);
		}

		String userId = user.getUserId();
		String userName = user.getUsername();

		if (!user.isActive()) {
			return validateDeletedUser(userId, userName);
		}

		if (!user.isVerified()) {
			return validateUnVerifiedUser(userId, userName);
		}

		return validateValidUser(userId, userName);
	}


	@Override
	public ValidationResult validateObjectByUserId(UserRequest userReq, boolean requiresPasswordValidation) {
		User user = userService.findByUserId(userReq.getUserId());

		if (user == null) {
			return validateUserNotFound(userReq.getUserId(), INVALID_USER_NAME);
		}

		String userName = user.getUsername();

		if (!user.isActive()) {
			return validateDeletedUser(userReq.getUserId(), userName);
		}

		if (!user.isVerified()) {
			return validateUnVerifiedUser(userReq.getUserId(), userName);
		}
		
		if(requiresPasswordValidation) {
		String msgPassword = passwordValidatorService.validatePassword(userReq.getPassword());

		if (!msgPassword.equalsIgnoreCase("Valid")) {
			ValidationResult validationResult = new ValidationResult();
			validationResult.setMessage(msgPassword);
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			validationResult.setValid(false);
			validationResult.setUserId(userReq.getUserId());
			validationResult.setUserName(userReq.getUsername());
			return validationResult;
		}
		}

		return validateValidUser(userReq.getUserId(), userName);
	}
	
	private ValidationResult validateValidUser(String userId, String userName) {

		ValidationResult validationResult = new ValidationResult();
		validationResult.setUserId(userId);
		validationResult.setUserName(userName);
		validationResult.setValid(true);
		return validationResult;
	}
	
	private ValidationResult validateUserNotFound(String userId, String userName) {

		ValidationResult validationResult = new ValidationResult();
		validationResult.setMessage("User account not found.");
		validationResult.setStatus(HttpStatus.NOT_FOUND);
		validationResult.setValid(false);
		validationResult.setUserName(userName);
		validationResult.setUserId(userId);
		return validationResult;

	}

	private ValidationResult validateDeletedUser(String userId, String userName) {

		ValidationResult validationResult = new ValidationResult();
		validationResult.setMessage("User account is deleted.");
		validationResult.setStatus(HttpStatus.FORBIDDEN);
		validationResult.setValid(false);
		validationResult.setUserName(userName);
		validationResult.setUserId(userId);
		return validationResult;

	}

	private ValidationResult validateUnVerifiedUser(String userId, String userName) {

		ValidationResult validationResult = new ValidationResult();
		validationResult.setMessage("Please verify the account first.");
		validationResult.setStatus(HttpStatus.UNAUTHORIZED);
		validationResult.setValid(false);
		validationResult.setUserName(userName);
		validationResult.setUserId(userId);
		return validationResult;

	}

	@Override
	public ValidationResult validateObject(UserRequest userReq, String header) {
		ValidationResult validationResult = new ValidationResult();

		if (userReq.getEmail() == null || userReq.getEmail().isEmpty()) {

			String userName = StringUtils.hasText(userReq.getUsername()) ? userReq.getUsername()
					: INVALID_USER_NAME;
			validationResult.setMessage("Email cannot be empty.");
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			validationResult.setValid(false);
			validationResult.setUserId(INVALID_USER_ID);
			validationResult.setUserName(userName);
			return validationResult;
		}

		User dbUser = userService.findByEmail(userReq.getEmail());
		if (dbUser != null) {
			validationResult.setMessage(userReq.getEmail() + " is existed.");
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			validationResult.setValid(false);
			validationResult.setUserId(dbUser.getUserId());
			validationResult.setUserName(dbUser.getUsername());
			return validationResult;
		}
		
		boolean invited = userInvitationService.existsByEmailIsUsed(userReq.getEmail());
		if (invited) {
			validationResult.setMessage("Invitation already sent to "+userReq.getEmail());
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			validationResult.setValid(false);
			validationResult.setUserId(dbUser.getUserId());
			validationResult.setUserName(dbUser.getUsername());
			return validationResult;
		}
		
		if (userReq.getRole() == null || userReq.getRole().isEmpty()) {
			validationResult.setMessage("Role cannot be empty.");
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			validationResult.setValid(false);
			return validationResult;
		}else {
		
		RoleDTO roleDTO = roleService.findByRoleName(userReq.getRole());

		if (roleDTO == null || roleDTO.getRoleId() == null || roleDTO.getRoleId().isEmpty()) {
			validationResult.setMessage("Invalid role: " + userReq.getRole());
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			validationResult.setValid(false);
			return validationResult;
		}
		}
		

		if (userReq.getOrganizationId() == null || userReq.getOrganizationId().isEmpty()) {
			String userName = StringUtils.hasText(userReq.getUsername()) ? userReq.getUsername()
					: INVALID_USER_NAME;
			validationResult.setMessage("Organization cannot be empty.");
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			validationResult.setValid(false);
			validationResult.setUserId(INVALID_USER_ID);
			validationResult.setUserName(userName);
			return validationResult;
		}else {
			if(header != null && !header.equals("")) {
			JSONObject jsonResponse  = jsonReader.getOrganization(userReq.getOrganizationId(), header);
			Boolean getSuccessFromResponse =jsonReader.getSuccessFromResponse(jsonResponse);
			if(!getSuccessFromResponse) {
				String userName = StringUtils.hasText(userReq.getUsername()) ? userReq.getUsername()
						: INVALID_USER_NAME;
				validationResult.setMessage("Organization is not valid.");
				validationResult.setStatus(HttpStatus.BAD_REQUEST);
				validationResult.setValid(false);
				validationResult.setUserId(INVALID_USER_ID);
				validationResult.setUserName(userName);
				return validationResult;
			}
			}
			
		}

		validationResult.setValid(true);
		return validationResult;
	}

	
}
