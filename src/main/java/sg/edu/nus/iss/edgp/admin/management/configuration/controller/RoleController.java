package sg.edu.nus.iss.edgp.admin.management.configuration.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import sg.edu.nus.iss.edgp.admin.management.configuration.service.impl.RoleService;
import sg.edu.nus.iss.edgp.admin.management.dto.APIResponse;
import sg.edu.nus.iss.edgp.admin.management.dto.RoleDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.ValidationResult;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;
import sg.edu.nus.iss.edgp.admin.management.enums.HTTPVerb;
import sg.edu.nus.iss.edgp.admin.management.strategy.impl.RoleValidationStrategy;

@AllArgsConstructor
@RestController
@RequestMapping("/api/admin/roles")
@Validated
public class RoleController {
	
	private static final Logger logger = LoggerFactory.getLogger(RoleController.class);
	
	private static final String INVALID_USER_ID = "Invalid UserID";
	private static final String API_ADMIN_ROLES_ENDPOINT = "/api/admin/roles";
	
	private final RoleService roleService;
	private final RoleValidationStrategy roleValidationStrategy;
	private final APIResponse<RoleDTO> apiResponse;
	 
	public ResponseEntity <APIResponse<RoleDTO>>createRole(@RequestHeader("Authorization") String authorizationHeader,@RequestPart("role") Role role)
	{

	logger.info("Call role create API...");
	String message = "";
	String activityType = "CreatRole";
	String endpoint = API_ADMIN_ROLES_ENDPOINT;
	HTTPVerb httpMethod = HTTPVerb.POST;
	String userid = INVALID_USER_ID;
	
	try {
		ValidationResult validationResult = roleValidationStrategy.validateCreation(role,authorizationHeader);

		if (validationResult.isValid()) {
		RoleDTO roleDTO = roleService.createRole(role);
		message = roleDTO.getRoleName() + " is created successfully.";
		return apiResponse.handleResponseAndSendAudtiLogForSuccessCase(userid, activityType, endpoint, httpMethod, message,
				roleDTO, authorizationHeader);
		

		} else {
			return apiResponse.handleResponseAndSendAudtiLogForFailureCase(userid, activityType, endpoint, httpMethod,
					validationResult.getMessage(), validationResult.getStatus(), "", authorizationHeader);
		}

	} catch (Exception ex) {
		message = "The attempt to create role was unsuccessful.";
		return apiResponse.handleResponseAndSendAudtiLogForFailureCase(userid, activityType, endpoint, httpMethod, message,
				HttpStatus.INTERNAL_SERVER_ERROR, ex.toString(), authorizationHeader);
	}
	}
}
