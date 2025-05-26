package sg.edu.nus.iss.edgp.admin.management.dto;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import sg.edu.nus.iss.edgp.admin.management.enums.HTTPVerb;

@Data
@Builder
@AllArgsConstructor
public class APIResponse<T> {

	private Boolean success;
	private String message;
	private long totalRecord;
	private T data;


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
	

	public ResponseEntity<APIResponse<T>> handleResponseAndSendAudtiLogForSuccessCase(String userid,
			String activityType, String endpoint, HTTPVerb httpMethod, String message2, RoleDTO roleDTO,
			String authorizationHeader) {
		// TODO Auto-generated method stub
		return null;
	}

	public ResponseEntity<APIResponse<T>> handleResponseAndSendAudtiLogForFailureCase(String userid,
			String activityType, String endpoint, HTTPVerb httpMethod, String message2, HttpStatus status,
			String string, String authorizationHeader) {
         int httpStatusCode = status.value();
		
		return ResponseEntity.status(httpStatusCode).body(APIResponse.error(message));
	}
	

}