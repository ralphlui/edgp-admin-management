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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import sg.edu.nus.iss.edgp.admin.management.dto.APIResponse;
import sg.edu.nus.iss.edgp.admin.management.dto.AuditDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.PermissionDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.RoleDTO;
import sg.edu.nus.iss.edgp.admin.management.enums.AuditLogInvalidUser;
import sg.edu.nus.iss.edgp.admin.management.enums.HTTPVerb;
import sg.edu.nus.iss.edgp.admin.management.jwt.JWTService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.AuditService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.PermissionService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/permissions")
@Validated
public class PermissionController {
	
	private static final Logger logger = LoggerFactory.getLogger(PermissionController.class);	 
	private static final String INVALID_USER_ID = AuditLogInvalidUser.INVALID_USER_ID.toString();	
	private static final String API_ENDPOINT = "/api/admin/permissions";
	private static final String UNEXPECTED_ERROR = "An unexpected error occurred. Please contact support.";
	private static final String LOG_MESSAGE_FORMAT = "{} {}";
	
	
	@Autowired
	private PermissionService permissionService;
	
	
	@Autowired
	private AuditService auditService;

	@Value("${audit.activity.type.prefix}")
	String activityTypePrefix;
	
	
	@GetMapping(value = "", produces = "application/json")
	public ResponseEntity<APIResponse<List<PermissionDTO>>> getAllPermissionList(
			@RequestHeader("Authorization") String authorizationHeader) {

		final String activityType = "GetAllPermissionList";

		final HTTPVerb httpMethod = HTTPVerb.GET;
	 
		String message = "";
		AuditDTO auditDTO = auditService.createAuditDTO(INVALID_USER_ID, activityType, activityTypePrefix, API_ENDPOINT, httpMethod);

		try {

			List<PermissionDTO> permissions = permissionService.findPermission();

			if (!permissions.isEmpty()) {
				message = "Successfully retrieved all permission.";
				auditService.logAudit(auditDTO, 200, message, authorizationHeader);
				return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(permissions, message,permissions.size()));
				
			} else {
				message = "No Active Permission List.";
				auditService.logAudit(auditDTO, 200, message, authorizationHeader);
				return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(permissions, message,permissions.size()));
				
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
