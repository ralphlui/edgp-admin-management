package sg.edu.nus.iss.edgp.admin.management.controller;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sg.edu.nus.iss.edgp.admin.management.dto.APIResponse;
import sg.edu.nus.iss.edgp.admin.management.dto.AuditDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.RoleDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.ValidationResult;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;
import sg.edu.nus.iss.edgp.admin.management.enums.AuditLogInvalidUser;
import sg.edu.nus.iss.edgp.admin.management.enums.HTTPVerb;
import sg.edu.nus.iss.edgp.admin.management.jwt.JWTService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.AuditService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.RoleService;
import sg.edu.nus.iss.edgp.admin.management.strategy.impl.RoleValidationStrategy;
import sg.edu.nus.iss.edgp.admin.management.utility.GeneralUtility;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/roles")
@Validated
public class RoleController {

	private static final Logger logger = LoggerFactory.getLogger(RoleController.class);	 
	private static final String INVALID_USER_ID = AuditLogInvalidUser.INVALID_USER_ID.toString();	
	private static final String API_ENDPOINT = "/api/admin/roles";
	private static final String UNEXPECTED_ERROR = "An unexpected error occurred. Please contact support.";
	private static final String LOG_MESSAGE_FORMAT = "{} {}";
	
	
	@Autowired
	private  RoleService roleService;
	
	
	@Autowired
	private RoleValidationStrategy roleValidationStrategy;
	
	@Autowired
	private AuditService auditService;

	@Value("${audit.activity.type.prefix}")
	String activityTypePrefix;
	
	
	@PostMapping(value = "", produces = "application/json")
	public ResponseEntity<APIResponse<RoleDTO>> createRole(
			@RequestBody Role role) {

		logger.info("Call role create API...");
		String message = "";
		String activityType = "CreatRole";
		String endpoint = API_ENDPOINT;
		HTTPVerb httpMethod = HTTPVerb.POST;
		 
		AuditDTO auditDTO = auditService.createAuditDTO(INVALID_USER_ID, activityType, activityTypePrefix, endpoint, httpMethod);

		try {
			 
			ValidationResult validationResult = roleValidationStrategy.validateCreation(role, "");

			if (validationResult.isValid()) {
				RoleDTO roleDTO = roleService.createRole(role);
				message = roleDTO.getRoleName() + " is created successfully.";
				auditService.logAudit(auditDTO, 200, message, "");
                return ResponseEntity.ok(APIResponse.success(roleDTO, message));
                

			} else {
				auditService.logAudit(auditDTO, 404, validationResult.getMessage(), "");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(APIResponse.error(validationResult.getMessage()));
               
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
	public ResponseEntity<APIResponse<RoleDTO>> updateRole(@RequestHeader("Authorization") String authorizationHeader,
			@RequestBody Role role) {

		logger.info("Calling Role update API...");

		String activityType = "Update Role";
		String endpoint = API_ENDPOINT ;
		HTTPVerb httpMethod = HTTPVerb.PUT;
		String message = "";
		
		AuditDTO auditDTO = auditService.createAuditDTO(INVALID_USER_ID, activityType, activityTypePrefix, endpoint, httpMethod);

		try {
			 
			String roleId = GeneralUtility.makeNotNull(role.getRoleId()).trim();

			if (!roleId.equals("")) {
				role.setRoleId(roleId);

				ValidationResult validationResult = roleValidationStrategy.validateUpdating(role, authorizationHeader);
				if (validationResult.isValid()) {
					RoleDTO roleDTO = roleService.updateRole(role,authorizationHeader);
					if (roleDTO != null && !roleDTO.getRoleId().isEmpty()) {
						message = roleDTO.getRoleName() + " is updated successfully.";
						auditService.logAudit(auditDTO, 200, message, authorizationHeader);
		                return ResponseEntity.ok(APIResponse.success(roleDTO, message));
		               

					} else {
						auditService.logAudit(auditDTO, 500, validationResult.getMessage(), authorizationHeader);
		                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(validationResult.getMessage()));
		                 
					}

				} else {
					auditService.logAudit(auditDTO, 400, validationResult.getMessage(), authorizationHeader);
	                return ResponseEntity.status(validationResult.getStatus()).body(APIResponse.error(validationResult.getMessage()));
	                
				}
			} else {
				message = "Bad Request:Campaign ID could not be blank.";
				logger.error(message);

				auditService.logAudit(auditDTO, 400, message, authorizationHeader);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(APIResponse.error(message));
                
			}

		} catch (Exception e) {
			message = UNEXPECTED_ERROR;
	        logger.error(LOG_MESSAGE_FORMAT, message, e.getMessage());
	        auditDTO.setRemarks(e.getMessage());
	        auditService.logAudit(auditDTO, 500, message, authorizationHeader);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(message));
		}
	}

	@GetMapping(value = "", produces = "application/json")
	public ResponseEntity<APIResponse<List<RoleDTO>>> getAllActiveRoleList(
			@RequestHeader("Authorization") String authorizationHeader) {

		final String activityType = "GetAllActiveRoleList";

		final HTTPVerb httpMethod = HTTPVerb.GET;
		String message = "";
		AuditDTO auditDTO = auditService.createAuditDTO(INVALID_USER_ID, activityType, activityTypePrefix, API_ENDPOINT, httpMethod);

		try {
			
			List<RoleDTO> roles = roleService.findByStatusTrue();

			if (!roles.isEmpty()) {
				message = "Successfully retrieved all active roles.";
				auditService.logAudit(auditDTO, 200, message, authorizationHeader);
				return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(roles, message,roles.size()));
				
			} else {
				message = "No Active Role List.";
				auditService.logAudit(auditDTO, 200, message, authorizationHeader);
				return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(roles, message,roles.size()));
				
			}

		} catch (Exception e) {
			message = UNEXPECTED_ERROR;
	        logger.error(LOG_MESSAGE_FORMAT, message, e.getMessage());
	        auditDTO.setRemarks(e.getMessage());
	        auditService.logAudit(auditDTO, 500, message, authorizationHeader);
	        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(APIResponse.error(message));
		}
	}

}
