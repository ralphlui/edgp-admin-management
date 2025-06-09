package sg.edu.nus.iss.edgp.admin.management.exception;

public class PermissionServiceException extends RuntimeException{
	
	public PermissionServiceException(String message) {
		 super(message);
	}
	
	public PermissionServiceException (String message, Throwable cause) {
       super(message, cause);
   }

}
