package sg.edu.nus.iss.edgp.admin.management.exception;

public class UserServiceException extends RuntimeException{
	public UserServiceException(String message) {
		 super(message);
	}
	
	public UserServiceException (String message, Throwable cause) {
       super(message, cause);
   }
}
