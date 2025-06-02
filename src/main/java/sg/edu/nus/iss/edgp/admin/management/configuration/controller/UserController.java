package sg.edu.nus.iss.edgp.admin.management.configuration.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import sg.edu.nus.iss.edgp.admin.management.dto.*;
import sg.edu.nus.iss.edgp.admin.management.entity.User;
import sg.edu.nus.iss.edgp.admin.management.enums.AuditLogInvalidUser;
import sg.edu.nus.iss.edgp.admin.management.service.impl.UserService;
import sg.edu.nus.iss.edgp.admin.management.strategy.impl.UserValidationStrategy;
import sg.edu.nus.iss.edgp.admin.management.utility.CookieUtils;
import sg.edu.nus.iss.edgp.admin.management.utility.GeneralUtility;
import sg.edu.nus.iss.edgp.admin.management.enums.HTTPVerb;
import sg.edu.nus.iss.edgp.admin.management.exception.UserNotFoundException;
import sg.edu.nus.iss.edgp.admin.management.jwt.JWTService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/users")
@Validated
public class UserController {
	
	private static final Logger logger = LoggerFactory.getLogger(UserController.class);
	
	private final UserValidationStrategy userValidationStrategy;
	private final UserService userService;
	private final JWTService jwtService;
	private final CookieUtils cookieUtils;
	
	private String INVALID_USER_ID = AuditLogInvalidUser.INVALID_USER_ID.toString();
	//private String INVALID_USER_NAME = AuditLogInvalidUser.INVALID_USER_NAME.toString();
	
	private static final String API_ENDPOINT = "api/admin/users";
	
	private final APIResponse<UserDTO> apiResponse = null;

	
	@PostMapping(value = "", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> createUser(@RequestHeader("Authorization") String authorizationHeader, @RequestBody UserRequest userRequest) {
		logger.info("Call user create API...");
		String message;
		String activityType = "Authentication-CreateUser";
		String endpoint = API_ENDPOINT;
		HTTPVerb httpMethod = HTTPVerb.POST;
		String userid = INVALID_USER_ID;	
		try {
			ValidationResult validationResult = userValidationStrategy.validateCreation(userRequest,authorizationHeader);
			
			userid = validationResult.getUserId();
			if (validationResult.isValid()) {
				
				UserDTO userDTO = userService.createUser(userRequest);
				message = userRequest.getEmail() + " is created successfully";
				return apiResponse.handleResponseAndSendAudtiLogForSuccessCase(userid, activityType, endpoint,
						httpMethod, message, userDTO, authorizationHeader,null);
				
			} else {
				return apiResponse.handleResponseAndSendAudtiLogForFailureCase(userid, activityType, endpoint,
						httpMethod, validationResult.getMessage(), validationResult.getStatus(), "",
						authorizationHeader);
			}
		} catch (Exception ex) {
			message = "An error has occurred while processing the create Role API request.";
			return apiResponse.handleResponseAndSendAudtiLogForFailureCase(userid, activityType, endpoint, httpMethod,
					message, HttpStatus.INTERNAL_SERVER_ERROR, ex.toString(), authorizationHeader);
		}

	}
	
	@PutMapping(value = "", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> updateUser(@RequestHeader("Authorization") String authorizationHeader,
			@RequestBody UserRequest userRequest) {
		logger.info("Call user update API...");
		String message;
		String activityType = "Authentication-UpdateUser";
		String apiEndPoint = String.format(API_ENDPOINT);
		HTTPVerb httpMethod = HTTPVerb.PUT;
		message = "Update User failed due to ";
		String loginUserId = INVALID_USER_ID;	
		
		try {
			String userID = userRequest.getUserId();
		    loginUserId = jwtService.retrieveUserID(authorizationHeader);
			ValidationResult validationResult = userValidationStrategy.validateUpdating(userRequest,authorizationHeader);

			if (validationResult.isValid()) {

				userRequest.setUserId(userID);
				UserDTO userDTO = userService.updateUser(userRequest);
				message = "User updated successfully.";
				return apiResponse.handleResponseAndSendAudtiLogForSuccessCase(loginUserId, activityType, apiEndPoint,
						httpMethod, message, userDTO, authorizationHeader,null);

			} else {
				return apiResponse.handleResponseAndSendAudtiLogForFailureCase(loginUserId, activityType, apiEndPoint,
						httpMethod, validationResult.getMessage(), validationResult.getStatus(), "",
						authorizationHeader);
			}
		} catch (Exception ex) {
			
			message = "An error has occurred while processing the create Role API request.";
			return apiResponse.handleResponseAndSendAudtiLogForFailureCase(loginUserId, activityType, apiEndPoint, httpMethod,
					message, HttpStatus.INTERNAL_SERVER_ERROR, ex.toString(), authorizationHeader);
		}
	}
	
	@GetMapping(value = "", produces = "application/json")
	public ResponseEntity<APIResponse<List<UserDTO>>> getAllActiveUsers(
			@RequestHeader("Authorization") String authorizationHeader, @Valid @ModelAttribute SearchRequest searchRequest) {
		logger.info("Call user getAll API with page={}, size={}", searchRequest.getPage(), searchRequest.getSize());
		String message = "";
		String activityType = "Authentication-RetrieveAllActiveUsers";
		String apiEndPoint = API_ENDPOINT;
		HTTPVerb httpMethod = HTTPVerb.GET;
	    message = "Retreving active user list is failed due to ";
		String userId = INVALID_USER_ID;
		
		try {
			
			userId = jwtService.retrieveUserID(authorizationHeader);
			Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), Sort.by("username").ascending());
			Map<Long, List<UserDTO>> resultMap = userService.findActiveUsers(pageable);
			logger.info("all active user list size {}", resultMap.size());

			Map.Entry<Long, List<UserDTO>> firstEntry = resultMap.entrySet().iterator().next();
			long totalRecord = firstEntry.getKey();
			List<UserDTO> users = firstEntry.getValue();

			logger.info("totalRecord: {}", totalRecord);
			logger.info("userDTO List");

			if (!users.isEmpty()) {
				message = "Successfully get all active verified user.";
				return apiResponse.handleResponseListAndSendAuditLogForSuccessCase(userId, activityType,
						apiEndPoint, httpMethod, message, users, users.size(), authorizationHeader,null);
			} else {
				message = "No Active User List.";
				return apiResponse.handleEmptyResponseListAndSendAuditLogForSuccessCase(userId, activityType,
						apiEndPoint, httpMethod, message, users, users.size(), authorizationHeader);
			}

		} catch (Exception ex) {
			message = "The attempt to retrieve active role list was unsuccessful.";
			return apiResponse.handleResponseListAndSendAuditLogForFailuresCase(userId, activityType,
					apiEndPoint, httpMethod, message, HttpStatus.INTERNAL_SERVER_ERROR, ex.toString(),
					authorizationHeader);
		}
	}
	
	@PatchMapping(value = "/verify", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> verifyUser(@RequestBody UserRequest userRequest) {

		String verifyid = userRequest.getAccountVerificationCode();
		logger.info("Call user verify API with verifyToken");
		verifyid = GeneralUtility.makeNotNull(verifyid);
		String message = "";
		String activityType = "Authentication-VerifyUser";
		String apiEndPoint = API_ENDPOINT+"/verify";
		HTTPVerb httpMethod = HTTPVerb.PATCH;
		message = "User verification is failed due to ";
		String auditLogUserId  =INVALID_USER_ID;
		try {

			if (!verifyid.isEmpty()) {
				UserDTO verifiedUserDTO = userService.verifyUser(verifyid);
			    auditLogUserId = verifiedUserDTO.getUserID();
				String auditLogUserName = verifiedUserDTO.getUsername();
				message = "User successfully verified.";
				return apiResponse.handleResponseAndSendAudtiLogForSuccessCase(auditLogUserId, activityType, apiEndPoint,
						httpMethod, message, verifiedUserDTO, "",null);
				
								
			} else {

				message = "Vefriy Id could not be blank.";
				logger.error(message);
				// To Do
				return apiResponse.handleResponseAndSendAudtiLogForFailureCase(auditLogUserId, activityType, apiEndPoint,
						httpMethod, message, HttpStatus.BAD_REQUEST, "",
						"");
				
			}
		} catch (Exception ex) {
			// To Do
			HttpStatusCode htpStatuscode = ex instanceof UserNotFoundException ? HttpStatus.NOT_FOUND
					: HttpStatus.INTERNAL_SERVER_ERROR;
			return apiResponse.handleResponseAndSendAudtiLogForFailureCase(auditLogUserId, activityType, apiEndPoint, httpMethod,
					message, htpStatuscode, ex.toString(), "");

	}

	}
	
	@PostMapping(value = "/login", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> loginUser(@RequestBody UserRequest userRequest) {
		logger.info("Call user login API...");
		String message = "";
		String activityType = "Authentication-LoginUser";
		String apiEndPoint = API_ENDPOINT+ "/login";
		HTTPVerb httpMethod = HTTPVerb.POST;
		message = "User failed to login due to ";
		String auditLogUserId  =INVALID_USER_ID;
		try {
			ValidationResult validationResult = userValidationStrategy.validateObject(userRequest.getEmail());
			auditLogUserId = validationResult.getUserId();
			String auditLogUserName = validationResult.getUserName();

			if (!validationResult.isValid()) {

				logger.error("Login Validation Error: {}", validationResult.getMessage());
				return apiResponse.handleResponseAndSendAudtiLogForFailureCase(auditLogUserId, activityType, apiEndPoint,
						httpMethod, validationResult.getMessage(), validationResult.getStatus(), "",
						"");
			}

			UserDTO userDTO = userService.loginUser(userRequest.getEmail(), userRequest.getPassword());
			message = userDTO.getEmail() + " login successfully";
 
		    HttpHeaders headers = cookieUtils.buildAuthHeadersWithCookies(userDTO.getUsername(), userDTO.getEmail(),
						userDTO.getUserID(), null);
				
		    return apiResponse.handleResponseAndSendAudtiLogForSuccessCase(auditLogUserId, activityType, apiEndPoint,
						httpMethod, message, userDTO, "",headers);
				 

		} catch (Exception ex) {
			HttpStatusCode htpStatuscode = ex instanceof UserNotFoundException ? HttpStatus.UNAUTHORIZED
					: HttpStatus.INTERNAL_SERVER_ERROR;
			return apiResponse.handleResponseAndSendAudtiLogForFailureCase(auditLogUserId, activityType, apiEndPoint, httpMethod,
					message, htpStatuscode, ex.toString(), "");
		}
	}
	
	@PostMapping(value = "/active", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> checkSpecificActiveUser(
			@RequestHeader("Authorization") String authorizationHeader,@RequestHeader("X-User-Id") String userID) {
		logger.info("Call user active API...");
		String message = "";
		String activityType = "Authentication-RetrieveActiveUserByUserId";
		String apiEndPoint = String.format("api/users/active");
		HTTPVerb httpMethod = HTTPVerb.GET;
		String activityDesc = "Retrieving active user by id failed due to ";
		
		HashMap<String,String> userInfo = new HashMap<String, String>();
		
		try {
			userInfo = userService.retrieveUserIDAndNameFromToken(authorizationHeader);
			User user  = userService.findActiveUserByID(userID);

			if (user == null) {
				message ="Active User not foud.";
				logger.error("Active User not foud.");
				return apiResponse.handleResponseAndSendAudtiLogForFailureCase(userInfo.get(INVALID_USER_ID), activityType, apiEndPoint,
						httpMethod, "", HttpStatus.NOT_FOUND, "",
						authorizationHeader); 

			}

			UserDTO userDTO = userService.checkSpecificActiveUser(userID);
			message = userDTO.getEmail() + " is Active";
			 
			return apiResponse.handleResponseAndSendAudtiLogForSuccessCase(userInfo.get(INVALID_USER_ID), activityType, apiEndPoint,
					httpMethod, message, userDTO, "",null);
		

		} catch (Exception ex) {
			// To Do
			HttpStatusCode htpStatuscode = ex instanceof UserNotFoundException ? HttpStatus.NOT_FOUND
					: HttpStatus.INTERNAL_SERVER_ERROR;
			return apiResponse.handleResponseAndSendAudtiLogForFailureCase(userInfo.get(INVALID_USER_ID), activityType, apiEndPoint, httpMethod,
					message, htpStatuscode, ex.toString(), "");
		}
	}
	


}
