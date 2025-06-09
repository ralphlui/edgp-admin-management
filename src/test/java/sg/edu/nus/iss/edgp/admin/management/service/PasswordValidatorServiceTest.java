package sg.edu.nus.iss.edgp.admin.management.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension; 
import org.springframework.test.context.ActiveProfiles; 

import sg.edu.nus.iss.edgp.admin.management.service.impl.PasswordValidatorService;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
public class PasswordValidatorServiceTest {
	
	@InjectMocks
    private PasswordValidatorService passwordValidatorService;

    @Test
    void testPasswordTooShort() {
        String result = passwordValidatorService.validatePassword("A1#d");
        assertEquals("Password must be within 8-30 characters long.", result);
    }

    @Test
    void testPasswordTooLong() {
        String longPassword = "A1#d".repeat(10);
        String result = passwordValidatorService.validatePassword(longPassword);
        assertEquals("Password must be within 8-30 characters long.", result);
    }

    @Test
    void testPasswordMissingUppercase() {
        String result = passwordValidatorService.validatePassword("abc#1234");
        assertEquals("Password must contain at least one uppercase letter.", result);
    }

    @Test
    void testPasswordMissingLowercase() {
        String result = passwordValidatorService.validatePassword("ABC#1234");
        assertEquals("Password must contain at least one lowercase letter.", result);
    }

    @Test
    void testPasswordMissingDigit() {
        String result = passwordValidatorService.validatePassword("Abc#defg");
        assertEquals("Password must contain at least one numeric digit.", result);
    }

    @Test
    void testPasswordMissingSpecialCharacter() {
        String result = passwordValidatorService.validatePassword("Abc12345");
        assertEquals("Password must contain at least one special character.", result);
    }

    @Test
    void testPasswordContainingDictionaryWord() {
        // Assuming "admin" is in dictionary.txt
        String result = passwordValidatorService.validatePassword("Adm1n#Pass");
        assertEquals("Password must not contain common dictionary words.", result);
    }

    @Test
    void testValidPassword() {
        String result = passwordValidatorService.validatePassword("12345678kH@");
        assertEquals("valid", result);
    }

    @Test
    void testContainsNonAlphanumericCharacters() {
        assertTrue(PasswordValidatorService.containsNonAlphanumericCharacters("abc$123"));
    }
}