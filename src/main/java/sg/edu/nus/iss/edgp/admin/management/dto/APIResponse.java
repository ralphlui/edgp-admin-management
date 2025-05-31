package sg.edu.nus.iss.edgp.admin.management.dto;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import sg.edu.nus.iss.edgp.admin.management.enums.AuditLogResponseStatus;
import sg.edu.nus.iss.edgp.admin.management.enums.HTTPVerb;
import sg.edu.nus.iss.edgp.admin.management.exception.UserNotFoundException;
import sg.edu.nus.iss.edgp.admin.management.strategy.impl.AuditService;

@Data
@Builder
@AllArgsConstructor
public class APIResponse<T> {

	private Boolean success;
	private String message;
	private long totalRecord;
	private T data;

	private AuditService auditService;
	
	private String auditLogResponseSuccess = AuditLogResponseStatus.SUCCESS.toString();
	private String auditLogResponseFailure = AuditLogResponseStatus.FAILED.toString();
	private String genericErrorMessage = "An error occurred while processing your request. Please try again later.";
	
	
	@Value("${audit.activity.type.prefix}")
	String activityTypePrefix;
	
	public static <T> APIResponse<T> success(String message) {
		return APIResponse.<T>builder().success(true).message(message).data(null).totalRecord(1).build();
	}

	public static <T> APIResponse<T> error(T data) {
		return APIResponse.<T>builder().success(false).message("error").totalRecord(0).build();
	}

	public static <T> APIResponse<T> error(String message) {
		return APIResponse.<T>builder().success(false).message(message).totalRecord(0).build();
	}

	public static <T> APIResponse<T> success(T data, String message) {
		return APIResponse.<T>builder().success(true).message(message).data(data).totalRecord(1).build();
	}


	public static <T> APIResponse<T> success(T data, String message, long totalRecord) {
		return APIResponse.<T>builder().success(true).message(message).totalRecord(totalRecord).data(data).build();
	}
	
	public static <T> APIResponse<T> noList(T data, String message) {
		return APIResponse.<T>builder().success(true).message(message).data(data).totalRecord(0).build();
	}

	public ResponseEntity<APIResponse<T>> handleResponseAndSendAudtiLogForSuccessCase(String userId,
			String activityType, String endpoint, HTTPVerb httpVerb, String message, T t,
			String authorizationHeader) {
		AuditDTO auditDTO = auditService.createAuditDTO(userId, activityType, activityTypePrefix, endpoint, httpVerb);
		auditService.logAudit(auditDTO, HttpStatus.OK.value(), message, authorizationHeader);
		return ResponseEntity.status(HttpStatus.OK).body(APIResponse.success(t, message));
	}

	public ResponseEntity<APIResponse<T>> handleResponseAndSendAudtiLogForFailureCase(String userId,
			String activityType, String endpoint, HTTPVerb httpVerb, String message, HttpStatusCode htpStatuscode,
			String remark, String authorizationHeader) {
		AuditDTO auditDTO = auditService.createAuditDTO(userId, activityType, activityTypePrefix, endpoint, httpVerb);
		auditDTO.setRemarks(remark);
		auditService.logAudit(auditDTO, htpStatuscode.value(), message, authorizationHeader);
		return ResponseEntity.status(htpStatuscode).body(APIResponse.error(message));
	}
	
	public ResponseEntity<APIResponse<List<T>>> handleResponseListAndSendAuditLogForFailuresCase(String userId,
			String activityType, String endpoint, HTTPVerb httpVerb, String message, HttpStatusCode htpStatuscode,
			String remark, String authorizationHeader) {
		
		int httpStatusCode = htpStatuscode.value();
		AuditDTO auditDTO = auditService.createAuditDTO(userId, activityType, activityTypePrefix, endpoint, httpVerb);
		auditDTO.setRemarks(remark);
		auditService.logAudit(auditDTO, httpStatusCode, message, authorizationHeader);
		return ResponseEntity.status(httpStatusCode).body(APIResponse.error(message));

	}
	
	public ResponseEntity<APIResponse<List<T>>> handleResponseListAndSendAuditLogForSuccessCase(String userId,
			String activityType, String endpoint, HTTPVerb httpVerb, String message, List<T> dtoList,
			long totalRecord, String authorizationHeader) {
		int httpStatusCode = HttpStatus.OK.value();
		AuditDTO auditDTO = auditService.createAuditDTO(userId, activityType, activityTypePrefix, endpoint, httpVerb);
		auditService.logAudit(auditDTO, httpStatusCode, message, authorizationHeader);
		return ResponseEntity.status(httpStatusCode).body(APIResponse.success(dtoList, message, totalRecord));

	}
	
	public ResponseEntity<APIResponse<List<T>>> handleEmptyResponseListAndSendAuditLogForSuccessCase(
			String userId, String activityType, String endpoint, HTTPVerb httpVerb, String message,
			List<T> storeDTOList, long totalRecord, String authorizationHeader) {
		int httpStatusCode = HttpStatus.OK.value();
		AuditDTO auditDTO = auditService.createAuditDTO(userId, activityType, activityTypePrefix, endpoint, httpVerb);
		auditService.logAudit(auditDTO, httpStatusCode, message, authorizationHeader);
		return ResponseEntity.status(httpStatusCode).body(APIResponse.noList(storeDTOList, message));

	}
}