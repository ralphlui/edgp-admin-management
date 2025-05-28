package sg.edu.nus.iss.edgp.admin.management.configuration.controller;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import sg.edu.nus.iss.edgp.admin.management.dto.APIResponse;
import sg.edu.nus.iss.edgp.admin.management.dto.RoleDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.ValidationResult;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;
import sg.edu.nus.iss.edgp.admin.management.enums.HTTPVerb;
import sg.edu.nus.iss.edgp.admin.management.jwt.JWTService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.RoleService;
import sg.edu.nus.iss.edgp.admin.management.strategy.impl.RoleValidationStrategy;
import sg.edu.nus.iss.edgp.admin.management.utility.GeneralUtility;


@RestController
@RequestMapping("/api/admin/roles")
@Validated
public class RoleController {

	private static final Logger logger = LoggerFactory.getLogger(RoleController.class);

	private static final String INVALID_USER_ID = "Invalid UserID";
	private static final String API_ADMIN_ROLES_ENDPOINT = "/api/admin/roles";
	
	@Autowired
	private  RoleService roleService;
	
	@Autowired
	private JWTService jwtService;
	
	@Autowired
	private RoleValidationStrategy roleValidationStrategy;
	
	
	private final APIResponse<RoleDTO> apiResponse = null;

	public ResponseEntity<APIResponse<RoleDTO>> createRole(@RequestHeader("Authorization") String authorizationHeader,
			@RequestPart("role") Role role) {

		logger.info("Call role create API...");
		String message = "";
		String activityType = "CreatRole";
		String endpoint = API_ADMIN_ROLES_ENDPOINT + "/create";
		HTTPVerb httpMethod = HTTPVerb.POST;
		String userid = INVALID_USER_ID;

		try {
			userid = jwtService.retrieveUserID(authorizationHeader);
			ValidationResult validationResult = roleValidationStrategy.validateCreation(role, authorizationHeader);

			if (validationResult.isValid()) {
				RoleDTO roleDTO = roleService.createRole(role);
				message = roleDTO.getRoleName() + " is created successfully.";
				return apiResponse.handleResponseAndSendAudtiLogForSuccessCase(userid, activityType, endpoint,
						httpMethod, message, roleDTO, authorizationHeader);

			} else {
				return apiResponse.handleResponseAndSendAudtiLogForFailureCase(userid, activityType, endpoint,
						httpMethod, validationResult.getMessage(), validationResult.getStatus(), "",
						authorizationHeader);
			}

		} catch (Exception ex) {
			message = "An error has occurred while processing the create Role API request.";

			logger.info(message);

			return apiResponse.handleResponseAndSendAudtiLogForFailureCase(userid, activityType, endpoint, httpMethod,
					message, HttpStatus.INTERNAL_SERVER_ERROR, ex.toString(), authorizationHeader);
		}

	}

	@PutMapping(value = "/update", produces = "application/json")
	public ResponseEntity<APIResponse<RoleDTO>> updateRole(@RequestHeader("Authorization") String authorizationHeader,
			@RequestBody Role role) {

		logger.info("Calling Role update API...");

		String activityType = "Update Role";
		String endpoint = API_ADMIN_ROLES_ENDPOINT + "/update";
		HTTPVerb httpMethod = HTTPVerb.PUT;
		String message = "";
		String userid = INVALID_USER_ID;

		try {

			String roleId = GeneralUtility.makeNotNull(role.getRoleId()).trim();

			if (!roleId.equals("")) {
				role.setRoleId(roleId);

				ValidationResult validationResult = roleValidationStrategy.validateUpdating(role, authorizationHeader);
				if (validationResult.isValid()) {
					RoleDTO roleDTO = roleService.updateRole(role);
					if (roleDTO != null && !roleDTO.getRoleId().isEmpty()) {
						message = roleDTO.getRoleName() + " is updated successfully.";
						return apiResponse.handleResponseAndSendAudtiLogForSuccessCase(userid, activityType, endpoint,
								httpMethod, message, roleDTO, authorizationHeader);

					} else {

						message = "The update for the campaign has failed. Please check the provided Role :"
								+ role.getRoleName();
						logger.error("Calling Role update API failed...");

						return apiResponse.handleResponseAndSendAudtiLogForFailureCase(userid, activityType, endpoint,
								httpMethod, message, HttpStatus.INTERNAL_SERVER_ERROR, "", authorizationHeader);
					}

				} else {
					return apiResponse.handleResponseAndSendAudtiLogForFailureCase(userid, activityType, endpoint,
							httpMethod, validationResult.getMessage(), validationResult.getStatus(), "",
							authorizationHeader);
				}
			} else {
				message = "Bad Request:Campaign ID could not be blank.";
				logger.error(message);

				return apiResponse.handleResponseAndSendAudtiLogForFailureCase(userid, activityType, endpoint,
						httpMethod, message, HttpStatus.BAD_REQUEST, "", authorizationHeader);
			}

		} catch (Exception ex) {
			message = "An error has occurred while processing the update Role API request.";

			logger.info(message);

			return apiResponse.handleResponseAndSendAudtiLogForFailureCase(userid, activityType, endpoint, httpMethod,
					message, HttpStatus.INTERNAL_SERVER_ERROR, ex.toString(), authorizationHeader);
		}
	}

	@GetMapping(value = "", produces = "application/json")
	public ResponseEntity<APIResponse<List<RoleDTO>>> getAllActiveRoleList(
			@RequestHeader("Authorization") String authorizationHeader) {

		final String activityType = "GetAllActiveRoleList";

		final HTTPVerb httpMethod = HTTPVerb.GET;
		String userId = INVALID_USER_ID;
		String message = "";

		try {
			userId = jwtService.retrieveUserID(authorizationHeader);

			List<RoleDTO> roles = roleService.findByStatusTrue();

			if (!roles.isEmpty()) {
				message = "Successfully retrieved all active roles.";
				return apiResponse.handleResponseListAndSendAuditLogForSuccessCase(userId, activityType,
						API_ADMIN_ROLES_ENDPOINT, httpMethod, message, roles, roles.size(), authorizationHeader);
			} else {
				message = "No Active Role List.";
				return apiResponse.handleEmptyResponseListAndSendAuditLogForSuccessCase(userId, activityType,
						API_ADMIN_ROLES_ENDPOINT, httpMethod, message, roles, roles.size(), authorizationHeader);
			}

		} catch (Exception e) {
			message = "The attempt to retrieve active store list was unsuccessful.";
			return apiResponse.handleResponseListAndSendAuditLogForFailuresCase(userId, activityType,
					API_ADMIN_ROLES_ENDPOINT, httpMethod, message, HttpStatus.INTERNAL_SERVER_ERROR, e.toString(),
					authorizationHeader);
		}
	}

}
