package sg.edu.nus.iss.edgp.admin.management.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserServiceExceptionTest {

	@Test
	void testConstructor() {

		String errorMessage = "Invalid User";
		UserServiceException exception = new UserServiceException(errorMessage);

		assertEquals(errorMessage, exception.getMessage());
	}

	@Test
	void testConstructorWithMessageAndCause() {
		String message = "Invalid User";
		Throwable cause = new RuntimeException("Database error");
		UserServiceException exception = new UserServiceException(message, cause);

		assertEquals(message, exception.getMessage());
		assertEquals(cause, exception.getCause());
	}
}
