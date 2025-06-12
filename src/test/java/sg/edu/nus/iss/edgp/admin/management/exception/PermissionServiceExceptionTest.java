package sg.edu.nus.iss.edgp.admin.management.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PermissionServiceExceptionTest {
	@Test
	void testConstructor() {

		String errorMessage = "Permission not found";
		PermissionServiceException exception = new PermissionServiceException(errorMessage);

		assertEquals(errorMessage, exception.getMessage());
	}

	@Test
	void testConstructorWithMessageAndCause() {
		String message = "Permission not found";
		Throwable cause = new RuntimeException("Database error");
		PermissionServiceException exception = new PermissionServiceException(message, cause);

		assertEquals(message, exception.getMessage());
		assertEquals(cause, exception.getCause());
	}

}
