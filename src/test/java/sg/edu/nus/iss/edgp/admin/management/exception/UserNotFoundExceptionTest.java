package sg.edu.nus.iss.edgp.admin.management.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class UserNotFoundExceptionTest {

	@Test
    void testConstructor() {
      
        String errorMessage = "User not found";
        UserNotFoundException exception = new UserNotFoundException(errorMessage);
 
        assertEquals(errorMessage, exception.getMessage());
    }
	
}
