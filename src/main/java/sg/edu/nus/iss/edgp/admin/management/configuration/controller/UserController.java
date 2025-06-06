package sg.edu.nus.iss.edgp.admin.management.configuration.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import sg.edu.nus.iss.edgp.admin.management.dto.*;
import sg.edu.nus.iss.edgp.admin.management.entity.RefreshToken;
import sg.edu.nus.iss.edgp.admin.management.entity.User;
import sg.edu.nus.iss.edgp.admin.management.entity.UserInvitation;
import sg.edu.nus.iss.edgp.admin.management.enums.AuditLogInvalidUser;
import sg.edu.nus.iss.edgp.admin.management.service.impl.AuditService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.RefreshTokenService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.UserInvitationService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.UserService;
import sg.edu.nus.iss.edgp.admin.management.strategy.impl.UserValidationStrategy;
import sg.edu.nus.iss.edgp.admin.management.utility.CookieUtils;
import sg.edu.nus.iss.edgp.admin.management.utility.DTOMapper;
import sg.edu.nus.iss.edgp.admin.management.utility.GeneralUtility;
import sg.edu.nus.iss.edgp.admin.management.enums.HTTPVerb;
import sg.edu.nus.iss.edgp.admin.management.exception.UserNotFoundException;
import sg.edu.nus.iss.edgp.admin.management.jwt.JWTService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users")
@Validated
public class UserController {

	private static final Logger logger = LoggerFactory.getLogger(UserController.class);

	private final UserValidationStrategy userValidationStrategy;
	private final UserService userService;
	private final UserInvitationService userInvitationService;
	private final JWTService jwtService;
	private final CookieUtils cookieUtils;
	private final RefreshTokenService refreshTokenService;

	@Autowired
	private AuditService auditService;

	private String INVALID_USER_ID = AuditLogInvalidUser.INVALID_USER_ID.toString();
	private String INVALID_USER_NAME = AuditLogInvalidUser.INVALID_USER_NAME.toString();
	private static final String ACCESS_TOKEN_COOKIE = "access_token";
	private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

	private static final String API_ENDPOINT = "api/admin/users";
	private static final String UNEXPECTED_ERROR = "An unexpected error occurred. Please contact support.";
	private static final String LOG_MESSAGE_FORMAT = "{} {}";

	@Value("${audit.activity.type.prefix}")
	String activityTypePrefix;

	@PostMapping(value = "", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> createUser(@RequestBody UserRequest userRequest) {
		logger.info("Call user create API...");
		String message;
		String activityType = "Authentication-CreateUser";
		String endpoint = API_ENDPOINT;
		HTTPVerb httpMethod = HTTPVerb.POST;

		AuditDTO auditDTO = auditService.createAuditDTO(INVALID_USER_ID, activityType, activityTypePrefix, endpoint,
				httpMethod);

		try {
			ValidationResult validationResult = userValidationStrategy.validateCreation(userRequest, "");

			if (validationResult.isValid()) {

				UserDTO userDTO = userService.createUser(userRequest);
				message = userRequest.getEmail() + " is created successfully";
				auditService.logAudit(auditDTO, 200, message, "");
				return ResponseEntity.ok(APIResponse.success(userDTO, message));

			} else {

				auditService.logAudit(auditDTO, 404, validationResult.getMessage(), "");
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(APIResponse.error(validationResult.getMessage()));

			}
		} catch (Exception e) {

			message = UNEXPECTED_ERROR;
			logger.error(LOG_MESSAGE_FORMAT, message, e.getMessage());
			auditDTO.setRemarks(e.getMessage());
			auditService.logAudit(auditDTO, 500, message, "");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(message));
		}

	}

	@PutMapping(value = "", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> updateUser(@RequestHeader("Authorization") String authorizationHeader,
			@RequestBody UserRequest userRequest) {
		logger.info("Call user update API...");
		String message;
		String activityType = "Authentication-UpdateUser";
		String endpoint = String.format(API_ENDPOINT);
		HTTPVerb httpMethod = HTTPVerb.PUT;
		message = "Update User failed due to ";

		AuditDTO auditDTO = auditService.createAuditDTO(INVALID_USER_ID, activityType, activityTypePrefix, endpoint,
				httpMethod);

		try {
			String userID = userRequest.getUserId();

			ValidationResult validationResult = userValidationStrategy.validateUpdating(userRequest,
					authorizationHeader);

			if (validationResult.isValid()) {

				userRequest.setUserId(userID);
				UserDTO userDTO = userService.updateUser(userRequest);
				message = "User updated successfully.";
				auditService.logAudit(auditDTO, 200, message, authorizationHeader);
				return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(userDTO, message));

			} else {
				auditService.logAudit(auditDTO, 404, validationResult.getMessage(), authorizationHeader);
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(APIResponse.error(validationResult.getMessage()));

			}
		} catch (Exception e) {

			logger.error(LOG_MESSAGE_FORMAT, message, e.getMessage());
			auditDTO.setRemarks(e.getMessage());
			auditService.logAudit(auditDTO, 500, message, "");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(message + e.getMessage()));
		}
	}

	@PostMapping(value = "/invite", produces = "application/json")
	public ResponseEntity<APIResponse<UserInvitationDTO>> inviteUser(
			@RequestHeader("Authorization") String authorizationHeader, @RequestBody UserRequest userRequest) {
		logger.info("Call user invite API...");
		String message;
		String activityType = "Authentication-InviteUser";
		String endpoint = API_ENDPOINT + "/inviteUser";
		HTTPVerb httpMethod = HTTPVerb.POST;

		AuditDTO auditDTO = auditService.createAuditDTO(INVALID_USER_ID, activityType, activityTypePrefix, endpoint,
				httpMethod);

		try {
			ValidationResult validationResult = userValidationStrategy.validateObject(userRequest, authorizationHeader);

			if (validationResult.isValid()) {

				UserInvitationDTO userInvitationDTO = userInvitationService.createInvitation(userRequest,
						authorizationHeader);
				if (userInvitationDTO != null) {

					message = userRequest.getEmail() + " is invited successfully";
					auditService.logAudit(auditDTO, 200, message, authorizationHeader);
					return ResponseEntity.ok(APIResponse.success(userInvitationDTO, message));
				} else {
					message = "User invitation is not successful";
					auditService.logAudit(auditDTO, 500, message, authorizationHeader);
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
							.body(APIResponse.error(validationResult.getMessage()));

				}

			} else {

				auditService.logAudit(auditDTO, 404, validationResult.getMessage(), authorizationHeader);
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(APIResponse.error(validationResult.getMessage()));

			}
		} catch (Exception e) {

			message = UNEXPECTED_ERROR;
			logger.error(LOG_MESSAGE_FORMAT, message, e.getMessage());
			auditDTO.setRemarks(e.getMessage());
			auditService.logAudit(auditDTO, 500, message, authorizationHeader);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(message));
		}

	}

	@PostMapping(value = "/complete-registration", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> completeRegistration(
			@RequestHeader("Authorization") String authorizationHeader, @RequestBody UserRequest userRequest) {
		String token = userRequest.getUserInvitationtoken();
		logger.info("Call complete registration API with user invitation Token");
		token = GeneralUtility.makeNotNull(token);
		String message = "";
		String activityType = "Authentication-CompleteRegistration";
		String endpoint = API_ENDPOINT + "/complete-registration";
		HTTPVerb httpMethod = HTTPVerb.POST;
		message = "User set password is failed due to ";

		AuditDTO auditDTO = auditService.createAuditDTO(INVALID_USER_ID, activityType, activityTypePrefix, endpoint,
				httpMethod);

		try {

			if (!token.isEmpty()) {

				UserInvitation invitation = userInvitationService.findByToken(token);

				if (invitation.isUsed() || invitation.getExpiresAt().isBefore(LocalDateTime.now())) {

					message = "Token has expired or already been used.";
					auditService.logAudit(auditDTO, 403, message, authorizationHeader);
					return ResponseEntity.status(HttpStatus.FORBIDDEN).body(APIResponse.error(message));

				}

				ValidationResult validationResult = userValidationStrategy.validateObject(userRequest.getEmail());

				if (validationResult.isValid()) {

					message = "User already exists.";
					auditService.logAudit(auditDTO, 409, message, authorizationHeader);
					return ResponseEntity.status(HttpStatus.CONFLICT).body(APIResponse.error(message));
				}

				UserDTO activatedUser = userService.accountActivate(userRequest);
				if (activatedUser != null) {
					UserInvitationDTO userInvitationDTO = userInvitationService.updateInvitation(userRequest);

					message = "Password set successfully. Account activated.";
					auditService.logAudit(auditDTO, 200, message, "");

					return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(activatedUser, message));

				} else {
					message = "Failed to set password.";
					logger.error(message);

					auditService.logAudit(auditDTO, 500, message, "");
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(message));

				}

			} else {

				message = "Token could not be blank.";
				logger.error(message);

				auditService.logAudit(auditDTO, 400, message, "");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(message));

			}
		} catch (Exception e) {
			logger.error(LOG_MESSAGE_FORMAT, message, e.getMessage());
			auditDTO.setRemarks(e.getMessage());
			auditService.logAudit(auditDTO, 500, message, "");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(message + e.getMessage()));
		}

	}

	@GetMapping(value = "", produces = "application/json")
	public ResponseEntity<APIResponse<List<UserDTO>>> getAllActiveUsers(
			@RequestHeader("Authorization") String authorizationHeader,
			@Valid @ModelAttribute SearchRequest searchRequest) {
		logger.info("Call user getAll API with page={}, size={}", searchRequest.getPage(), searchRequest.getSize());
		String message = "";
		String activityType = "Authentication-RetrieveAllActiveUsers";
		String endpoint = API_ENDPOINT;
		HTTPVerb httpMethod = HTTPVerb.GET;
		message = "Retreving active user list is failed due to ";

		AuditDTO auditDTO = auditService.createAuditDTO(INVALID_USER_ID, activityType, activityTypePrefix, endpoint,
				httpMethod);

		try {

			Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize(),
					Sort.by("username").ascending());
			Map<Long, List<UserDTO>> resultMap = userService.findActiveUsers(pageable);
			logger.info("all active user list size {}", resultMap.size());

			Map.Entry<Long, List<UserDTO>> firstEntry = resultMap.entrySet().iterator().next();
			long totalRecord = firstEntry.getKey();
			List<UserDTO> users = firstEntry.getValue();

			logger.info("totalRecord: {}", totalRecord);
			logger.info("userDTO List");

			if (!users.isEmpty()) {
				message = "Successfully get all active verified user.";

				auditService.logAudit(auditDTO, 200, message, authorizationHeader);
				return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(users, message, totalRecord));

			} else {
				message = "No Active User List.";
				auditService.logAudit(auditDTO, 200, message, authorizationHeader);
				return ResponseEntity.status(HttpStatus.OK).body(APIResponse.noList(users, message));

			}

		} catch (Exception e) {
			logger.error(LOG_MESSAGE_FORMAT, message, e.getMessage());
			auditDTO.setRemarks(e.getMessage());
			auditService.logAudit(auditDTO, 500, message, "");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(message + e.getMessage()));

		}
	}

	@PatchMapping(value = "/verify", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> verifyUser(@RequestBody UserRequest userRequest) {

		String verifyid = userRequest.getAccountVerificationCode();
		logger.info("Call user verify API with verifyToken");
		verifyid = GeneralUtility.makeNotNull(verifyid);
		String message = "";
		String activityType = "Authentication-VerifyUser";
		String endpoint = API_ENDPOINT + "/verify";
		HTTPVerb httpMethod = HTTPVerb.PATCH;
		message = "User verification is failed due to ";

		AuditDTO auditDTO = auditService.createAuditDTO(INVALID_USER_ID, activityType, activityTypePrefix, endpoint,
				httpMethod);

		try {

			if (!verifyid.isEmpty()) {
				UserDTO verifiedUserDTO = userService.verifyUser(verifyid);
				message = "User successfully verified.";
				auditService.logAudit(auditDTO, 200, message, "");

				return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(verifiedUserDTO, message));

			} else {

				message = "Vefriy Id could not be blank.";
				logger.error(message);
				// To Do
				auditService.logAudit(auditDTO, 404, message, "");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(APIResponse.error(message));

			}
		} catch (Exception e) {
			logger.error(LOG_MESSAGE_FORMAT, message, e.getMessage());
			auditDTO.setRemarks(e.getMessage());
			auditService.logAudit(auditDTO, 500, message, "");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(message + e.getMessage()));
		}

	}

	@PostMapping(value = "/login", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> loginUser(@RequestBody UserRequest userRequest) {
		logger.info("Call user login API...");
		String message = "";
		String activityType = "Authentication-LoginUser";
		String endpoint = API_ENDPOINT + "/login";
		HTTPVerb httpMethod = HTTPVerb.POST;
		message = "User failed to login due to ";

		AuditDTO auditDTO = auditService.createAuditDTO(INVALID_USER_ID, activityType, activityTypePrefix, endpoint,
				httpMethod);

		try {
			ValidationResult validationResult = userValidationStrategy.validateObject(userRequest.getEmail());

			if (!validationResult.isValid()) {

				logger.error("Login Validation Error: {}", validationResult.getMessage());
				auditService.logAudit(auditDTO, 404, validationResult.getMessage(), "");
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(APIResponse.error(validationResult.getMessage()));

			}

			UserDTO userDTO = userService.loginUser(userRequest.getEmail(), userRequest.getPassword());
			message = userDTO.getEmail() + " login successfully";

			HttpHeaders headers = cookieUtils.buildAuthHeadersWithCookies(userDTO, null);

			auditService.logAudit(auditDTO, 200, message, "");
			return ResponseEntity.status(HttpStatus.OK).headers(headers).body(APIResponse.success(userDTO, message));

		} catch (Exception e) {
			logger.error(LOG_MESSAGE_FORMAT, message, e.getMessage());
			auditDTO.setRemarks(e.getMessage());
			auditService.logAudit(auditDTO, 500, message, "");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(message + e.getMessage()));
		}
	}

	@GetMapping(value = "/profile", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> getUserProfile(
			@RequestHeader("Authorization") String authorizationHeader, @RequestHeader("X-User-Id") String userID) {
		logger.info("Call user active API...");
		String message = "";
		String activityType = "Authentication-RetrieveUserByUserId";
		String endpoint = String.format("api/users/profile");
		HTTPVerb httpMethod = HTTPVerb.GET;
		message = "Retrieving active user by id failed due to ";

		AuditDTO auditDTO = auditService.createAuditDTO(INVALID_USER_ID, activityType, activityTypePrefix, endpoint,
				httpMethod);

		try {
			User user = userService.findActiveUserByID(userID);

			if (user == null) {
				message = "Active User not foud.";
				logger.error("Active User not foud.");
				auditService.logAudit(auditDTO, 404, message, authorizationHeader);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(APIResponse.error(message));

			}

			UserDTO userDTO = userService.checkSpecificActiveUserByID(userID);
			message = userDTO.getEmail() + " is Active";

			auditService.logAudit(auditDTO, 200, message, "");
			return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(userDTO, message));

		} catch (Exception e) {
			logger.error(LOG_MESSAGE_FORMAT, message, e.getMessage());
			auditDTO.setRemarks(e.getMessage());
			auditService.logAudit(auditDTO, 500, message, "");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(message + e.getMessage()));
		}
	}

	@GetMapping(value = "/accessToken", produces = "application/json")
	public ResponseEntity<APIResponse<JWTDTO>> generateAccessToken(@RequestHeader("X-User-Email") String email) {

		String message = "";
		String activityType = "Authentication-AccessToken";
		String endpoint = API_ENDPOINT + "/accessToken";
		HTTPVerb httpMethod = HTTPVerb.GET;
		message = "Requesting new access token is failed due to ";

		AuditDTO auditDTO = auditService.createAuditDTO(INVALID_USER_ID, activityType, activityTypePrefix, endpoint,
				httpMethod);

		try {
			String userEmail = email;

			if (GeneralUtility.makeNotNull(userEmail).equals("")) {
				message = "Invalid user.";
				logger.info("Requesting access Token: {}", message);

				auditService.logAudit(auditDTO, 404, message, "");
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(APIResponse.error(message));
			}

			// find user
			UserDTO user = userService.checkSpecificActiveUserByEmail(userEmail);
			if (user == null) {
				message = "Invalid user.";
				auditService.logAudit(auditDTO, 400, message, "");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(message));

			}

			String accessToken = jwtService.generateToken(user);

			if (accessToken != null) {

				message = "Access token generated successfully.";
				JWTDTO jwtDTO = new JWTDTO();
				jwtDTO.setToken(accessToken);
				auditService.logAudit(auditDTO, 200, message, "");
				return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(jwtDTO, message));

			} else {

				message = "Failed to generate token.";
				logger.info("Requesting access Token: {}", message);
				auditService.logAudit(auditDTO, 401, message, "");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(message));

			}

		} catch (Exception e) {
			logger.error(LOG_MESSAGE_FORMAT, message, e.getMessage());
			auditDTO.setRemarks(e.getMessage());
			auditService.logAudit(auditDTO, 500, message, "");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(message + e.getMessage()));
		}

	}

	@PostMapping(value = "/refreshToken", produces = "application/json")
	public <T> ResponseEntity<APIResponse<T>> refreshToken(HttpServletRequest request, HttpServletResponse response) {

		String refreshToken = cookieUtils.getTokenFromCookies(request, REFRESH_TOKEN_COOKIE).orElse(null);
		String message = "";
		String activityType = "Authentication-RefreshToken";
		String endpoint = API_ENDPOINT + "/refreshToken";
		HTTPVerb httpMethod = HTTPVerb.GET;
		message = "Requesting new access token is failed due to ";
		AuditDTO auditDTO = auditService.createAuditDTO(INVALID_USER_ID, activityType, activityTypePrefix, endpoint,
				httpMethod);

		try {
			if (refreshToken == null) {
				message = "Refresh token is missing";
				logger.info("Requesting new access Token: {}", message);

				auditService.logAudit(auditDTO, 400, message, "");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(message));

			}

			RefreshToken savedRefreshToken = refreshTokenService.findRefreshToken(refreshToken);

			if (savedRefreshToken != null && refreshTokenService.verifyRefreshToken(savedRefreshToken)) {
				UserDTO userDTO = new UserDTO();
				userDTO.setUserID(savedRefreshToken.getUser().getUserId());
				userDTO.setUsername(savedRefreshToken.getUser().getUsername());
				userDTO.setEmail(savedRefreshToken.getUser().getEmail());
				User user = userService.findByUserIdAndStatus(savedRefreshToken.getUser().getUserId(), true, true);
				userDTO.setRole(user.getRole());

				HttpHeaders headers = cookieUtils.buildAuthHeadersWithCookies(userDTO, refreshToken);

				message = "Token refresh is successful.";

				refreshTokenService.updateRefreshToken(refreshToken, false);

				auditService.logAudit(auditDTO, 200, message, "");
				return ResponseEntity.status(HttpStatus.OK).headers(headers)
						.body(APIResponse.successWithNoData(message));

			} else {

				message = "Invalid or expired refresh token";
				logger.info("Requesting refresh Token: {} ", message);
				auditService.logAudit(auditDTO, 401, message, "");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(APIResponse.error(message));
			}

		} catch (Exception e) {
			logger.error(LOG_MESSAGE_FORMAT, message, e.getMessage());
			auditDTO.setRemarks(e.getMessage());
			auditService.logAudit(auditDTO, 500, message, "");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(message + e.getMessage()));

		}

	}

	@PatchMapping(value = "/resetPassword", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> resetPassword(@RequestBody UserRequest resetPwdReq) {

		logger.info("Call user resetPassword API...");

		String activityType = "Authentication-ResetPassword";
		String endpoint = API_ENDPOINT + "/resetPassword";
		HTTPVerb httpMethod = HTTPVerb.PATCH;
		String message = "Reset password is failed due to ";

		AuditDTO auditDTO = auditService.createAuditDTO(INVALID_USER_ID, activityType, activityTypePrefix, endpoint,
				httpMethod);
		try {
			ValidationResult validationResult = userValidationStrategy.validateObject(resetPwdReq.getEmail());
			if (!validationResult.isValid()) {
				logger.error("Reset passwrod validation is not successful");
				auditService.logAudit(auditDTO, 400, message, "");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(validationResult.getMessage()));

			}

			UserDTO userDTO = userService.resetPassword(resetPwdReq.getEmail(), resetPwdReq.getPassword());
			message = "Reset Password is completed.";

			auditService.logAudit(auditDTO, 200, message, "");
			return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(userDTO, message));

		} catch (Exception e) {
			logger.error(LOG_MESSAGE_FORMAT, message, e.getMessage());
			auditDTO.setRemarks(e.getMessage());
			auditService.logAudit(auditDTO, 500, message, "");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(APIResponse.error(message + e.getMessage()));
		}

	}

	@PostMapping(value = "/logout", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> lgoutUser(HttpServletRequest request) {
		logger.info("Call user update Preferences API...");
		String message;
		String activityType = "Authentication-Logout";
		String endpoint = API_ENDPOINT + "/logout";
		HTTPVerb httpMethod = HTTPVerb.POST;
		message = "Logging out user is failed due to ";
		String userID = AuditLogInvalidUser.INVALID_USER_ID.toString();
		AuditDTO auditDTO = auditService.createAuditDTO(INVALID_USER_ID, activityType, activityTypePrefix, endpoint,
				httpMethod);

		String tokenFromCookie = cookieUtils.getTokenFromCookies(request, ACCESS_TOKEN_COOKIE).orElse(null);

		ResponseCookie accessTokenCookie = cookieUtils.createCookie(ACCESS_TOKEN_COOKIE, "", true, 0);
		ResponseCookie refreshTokenCookie = cookieUtils.createCookie(REFRESH_TOKEN_COOKIE, "", true, 0);
		HttpHeaders headers = cookieUtils.createHttpHeader(accessTokenCookie, refreshTokenCookie);

		try {
			userID = jwtService.extractUserIdAllowExpiredToken(tokenFromCookie);
			User user = userService.findByUserId(userID);

			String refreshToken = cookieUtils.getTokenFromCookies(request, REFRESH_TOKEN_COOKIE).orElse(null);

			refreshTokenService.updateRefreshToken(refreshToken, true);

			if (user != null) {

				message = "User logout successfully";

				auditService.logAudit(auditDTO, 200, message, "");
				return ResponseEntity.status(HttpStatus.OK).headers(headers)
						.body(APIResponse.success(DTOMapper.toUserDTO(user), message));
			} else {
				message = "User not found, session cleared.";
				logger.error(message);
				auditService.logAudit(auditDTO, 404, message, tokenFromCookie);
				return ResponseEntity.status(HttpStatus.NOT_FOUND).body(APIResponse.error(message));

			}
		} catch (Exception e) {
			message = e.getMessage();
			String responseMessage = e instanceof UserNotFoundException ? e.getMessage() : UNEXPECTED_ERROR;
			logger.error(responseMessage);
			logger.error(LOG_MESSAGE_FORMAT, responseMessage, e.getMessage());
			auditDTO.setRemarks(e.getMessage());
			auditService.logAudit(auditDTO, 500, responseMessage, "");
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(responseMessage));
		}

	}

}
