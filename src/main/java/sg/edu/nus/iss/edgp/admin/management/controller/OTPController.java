package sg.edu.nus.iss.edgp.admin.management.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sg.edu.nus.iss.edgp.admin.management.dto.*;
import sg.edu.nus.iss.edgp.admin.management.enums.AuditLogInvalidUser;
import sg.edu.nus.iss.edgp.admin.management.enums.HTTPVerb;
import sg.edu.nus.iss.edgp.admin.management.exception.UserNotFoundException;
import sg.edu.nus.iss.edgp.admin.management.service.impl.*;
import sg.edu.nus.iss.edgp.admin.management.strategy.impl.UserValidationStrategy;
import sg.edu.nus.iss.edgp.admin.management.utility.CookieUtils;
import sg.edu.nus.iss.edgp.admin.management.utility.GeneralUtility;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/users/otp")
public class OTPController {
	private static final Logger logger = LoggerFactory.getLogger(OTPController.class);

	private final OTPService otpService;

	private final UserValidationStrategy userValidationStrategy;

	private final UserService userService;

	private final CookieUtils cookieUtils;

	private final AuditService auditService;

	private String INVALID_USER_ID = AuditLogInvalidUser.INVALID_USER_ID.toString();
	private String INVALID_USER_NAME = AuditLogInvalidUser.INVALID_USER_NAME.toString();

	@Value("${audit.activity.type.prefix}")
	String activityTypePrefix;

	@PostMapping(value = "/generate", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> generateOtp(@RequestBody UserRequest userRequest) {

		String message = "";
		String activityType = "Authentication-GenerateOTP";
		String endpoint = "/api/admin/users/otp/generate";
		HTTPVerb httpMethod = HTTPVerb.POST;
		String activityDesc = "Generating OTP is failed due to ";

		AuditDTO auditDTO = auditService.createAuditDTO(INVALID_USER_ID, activityType, activityTypePrefix, endpoint,
				httpMethod);

		try {
			// Check if userEmail is valid
			logger.info("Generate OTP request received.");

			ValidationResult validationResult = userValidationStrategy.validateObject(userRequest.getEmail());
			if (!validationResult.isValid()) {

				logger.error("Generate OTP validation is not successful");
				auditService.logAudit(auditDTO, 400, validationResult.getMessage(), "");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(validationResult.getMessage()));

			}

			auditDTO.setUserId(validationResult.getUserId());
			auditDTO.setUsername(validationResult.getUserName());

			String otp = otpService.generateOTP(userRequest.getEmail());

			if (!GeneralUtility.makeNotNull(otp).equals("")) {
				message = "OTP sent to " + userRequest.getEmail() + ". It is valid for 10 minutes.";
			}

			// To Call Notification API for Sent Email...

			UserDTO userDTO = userService.checkSpecificActiveUserByEmail(userRequest.getEmail());

			auditDTO.setActivityDescription(message);

			auditService.logAudit(auditDTO, 200, message, "");
			return ResponseEntity.ok(APIResponse.success(userDTO, message));

		} catch (Exception e) {
			message = "OTP code generation failed.";
			logger.error("generateOtp Error: {}", message);
			HttpStatusCode htpStatuscode = e instanceof UserNotFoundException ? HttpStatus.NOT_FOUND
					: HttpStatus.INTERNAL_SERVER_ERROR;

			auditDTO.setRemarks(e.getMessage());

			auditService.logAudit(auditDTO, Integer.parseInt(htpStatuscode.toString()), message, "");
			return ResponseEntity.status(htpStatuscode).body(APIResponse.error(message));
		}
	}

	@PostMapping(value = "/validate", produces = "application/json")
	public ResponseEntity<APIResponse<UserDTO>> validateOtp(@RequestBody UserRequest userRequest) {

		String message = "";
		String activityType = "Authentication-validate";
		String endpoint = "/api/admin/users/otp/generate";
		HTTPVerb httpMethod = HTTPVerb.POST;
		String activityDesc = "Validating OTP is failed due to ";
		AuditDTO auditDTO = auditService.createAuditDTO(INVALID_USER_ID, activityType, activityTypePrefix, endpoint,
				httpMethod);

		try {

			logger.info("OTP validation request received.");

			ValidationResult validationResult = userValidationStrategy.validateObject(userRequest.getEmail());
			if (!validationResult.isValid()) {

				logger.error("Generate OTP validation is not successful");

				auditService.logAudit(auditDTO, 500, validationResult.getMessage(), "");
				return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
						.body(APIResponse.error(validationResult.getMessage()));
			}

			auditDTO.setUserId(validationResult.getUserId());
			auditDTO.setUsername(validationResult.getUserName());
			boolean isValid = otpService.validateOTP(userRequest.getEmail(), userRequest.getOtp());
			UserDTO userDTO = userService.checkSpecificActiveUserByEmail(userRequest.getEmail());

			if (isValid) {
				message = "OTP is valid.";
				HttpHeaders headers = cookieUtils.buildAuthHeadersWithCookies(userDTO, null);

				auditDTO.setActivityDescription(message);
				auditService.logAudit(auditDTO, 200, message, "");
				return ResponseEntity.status(HttpStatus.OK).headers(headers)
						.body(APIResponse.success(userDTO, message));
			} else {
				message = "OTP expired or incorrect";
				auditDTO.setActivityDescription(message);
				auditService.logAudit(auditDTO, 400, message, "");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(APIResponse.error(message));
			}

		} catch (Exception e) {
			message = "OTP code validation failed.";

			logger.error("validateOtp Error: {}", message);
			HttpStatusCode htpStatuscode = e instanceof UserNotFoundException ? HttpStatus.NOT_FOUND
					: HttpStatus.INTERNAL_SERVER_ERROR;
			auditDTO.setRemarks(e.getMessage());

			auditService.logAudit(auditDTO, Integer.parseInt(htpStatuscode.toString()), message, "");

			return ResponseEntity.status(htpStatuscode).body(APIResponse.error(message));

		}
	}
}
