package sg.edu.nus.iss.edgp.admin.management.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RoleServiceExceptionTest {
 
@Test
void testConstructor() {
  
    String errorMessage = "Role not found";
    RoleServiceException exception = new RoleServiceException(errorMessage);

    assertEquals(errorMessage, exception.getMessage());
}

@Test
void testConstructorWithMessageAndCause() {
	String message = "Role not found";
	Throwable cause = new RuntimeException("Database error");
	RoleServiceException exception = new RoleServiceException(message,cause);

	assertEquals(message, exception.getMessage());
	assertEquals(cause, exception.getCause());
}
}
