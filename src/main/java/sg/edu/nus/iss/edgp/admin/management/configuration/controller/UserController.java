package sg.edu.nus.iss.edgp.admin.management.configuration.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import sg.edu.nus.iss.edgp.admin.management.dto.*; 
import sg.edu.nus.iss.edgp.admin.management.enums.AuditLogInvalidUser;
import sg.edu.nus.iss.edgp.admin.management.enums.AuditLogResponseStatus;
import sg.edu.nus.iss.edgp.admin.management.service.impl.UserService;
import sg.edu.nus.iss.edgp.admin.management.strategy.impl.UserValidationStrategy;
import sg.edu.nus.iss.edgp.admin.management.enums.HTTPVerb;
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
	private String auditLogResponseSuccess = AuditLogResponseStatus.SUCCESS.toString();
	private String auditLogResponseFailure = AuditLogResponseStatus.FAILED.toString();
	private String INVALID_USER_ID = AuditLogInvalidUser.INVALID_USER_ID.toString();
	private String INVALID_USER_NAME = AuditLogInvalidUser.INVALID_USER_NAME.toString();
	private String genericErrorMessage = "An error occurred while processing your request. Please try again later.";

	private static final String ACCESS_TOKEN_COOKIE = "access_token";
	private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
	private static final String API_ENDPOINT = "api/users";
	
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
						httpMethod, message, userDTO, authorizationHeader);
				
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
	
	@GetMapping(value = "", produces = "application/json")
	public ResponseEntity<APIResponse<List<UserDTO>>> getAllActiveUsers(
			@RequestHeader("Authorization") String authorizationHeader, @Valid @ModelAttribute SearchRequest searchRequest) {
		logger.info("Call user getAll API with page={}, size={}", searchRequest.getPage(), searchRequest.getSize());
		String message = "";
		String activityType = "Authentication-RetrieveAllActiveUsers";
		String apiEndPoint = API_ENDPOINT;
		HTTPVerb httpMethod = HTTPVerb.GET;
		String activityDesc = "Retreving active user list is failed due to ";
		String userId = INVALID_USER_ID;
		HashMap<String,String> userInfo = new HashMap<String, String>();
		
		
		try {
			
			userInfo = userService.retrieveUserIDAndNameFromToken(authorizationHeader);
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
						API_ENDPOINT, httpMethod, message, users, users.size(), authorizationHeader);
			} else {
				message = "No Active User List.";
				return apiResponse.handleEmptyResponseListAndSendAuditLogForSuccessCase(userId, activityType,
						API_ENDPOINT, httpMethod, message, users, users.size(), authorizationHeader);
			}

		} catch (Exception ex) {
			message = "The attempt to retrieve active role list was unsuccessful.";
			return apiResponse.handleResponseListAndSendAuditLogForFailuresCase(userId, activityType,
					API_ENDPOINT, httpMethod, message, HttpStatus.INTERNAL_SERVER_ERROR, ex.toString(),
					authorizationHeader);
		}
	}


}
