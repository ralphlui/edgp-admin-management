package sg.edu.nus.iss.edgp.admin.management.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;

import sg.edu.nus.iss.edgp.admin.management.dto.RoleDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.UserRequest;
import sg.edu.nus.iss.edgp.admin.management.dto.ValidationResult;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;
import sg.edu.nus.iss.edgp.admin.management.entity.User;
import sg.edu.nus.iss.edgp.admin.management.service.impl.PasswordValidatorService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.RoleService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.UserInvitationService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.UserService;
import sg.edu.nus.iss.edgp.admin.management.strategy.impl.UserValidationStrategy;
import sg.edu.nus.iss.edgp.admin.management.utility.DTOMapper;
import sg.edu.nus.iss.edgp.admin.management.utility.JSONReader;

class UserValidationStrategyTest {
	

	    @Mock
	    private UserService userService;

	    @Mock
	    private UserInvitationService userInvitationService;

	    @Mock
	    private PasswordValidatorService passwordValidatorService;

	    @Mock
	    private RoleService roleService;

	    @Mock
	    private JSONReader jsonReader;

	    @InjectMocks
	    private UserValidationStrategy userValidationStrategy;

	    private UserRequest userRequest;
	    
	    private Role role;
	    RoleDTO roleDTO ;
  

	    @BeforeEach
	    void setUp() {
	        MockitoAnnotations.openMocks(this);
	        userRequest = new UserRequest();
	        userRequest.setEmail("test@example.com");
	        userRequest.setPassword("Password123");
	        userRequest.setRole("Admin");
	        userRequest.setOrganizationId("org123");
	        userRequest.setUserId("u1");
	        userRequest.setUsername("testuser");
	        role = new Role("1", "OrgAdmin", "Organization Admin", true, LocalDateTime.now(), null, null, null);
	        roleDTO =  DTOMapper.toRoleDTO(role);
	    }

	    @Test
	    void testValidateCreation_valid() {
	    	
	    	JSONObject json = new JSONObject();
	    	json.put("success", true);
	        when(userService.findByEmail("test@example.com")).thenReturn(null);
	        when(passwordValidatorService.validatePassword("Password123")).thenReturn("Valid");
	        when(roleService.findByRoleName("Admin")).thenReturn(roleDTO);
	        when(jsonReader.getOrganization("org123", "token")).thenReturn(json);
	        when(jsonReader.getSuccessFromResponse(any())).thenReturn(true);

	        ValidationResult result = userValidationStrategy.validateCreation(userRequest, "token");
	        assertTrue(result.isValid());
	    }

	    @Test
	    void testValidateCreation_emailEmpty() {
	        userRequest.setEmail("");
	        ValidationResult result = userValidationStrategy.validateCreation(userRequest, "token");
	        assertFalse(result.isValid());
	        assertEquals(HttpStatus.BAD_REQUEST, result.getStatus());
	        assertEquals("Email cannot be empty.", result.getMessage());
	    }

	    @Test
	    void testValidateCreation_existingEmail() {
	        when(userService.findByEmail("test@example.com")).thenReturn(new User());
	        ValidationResult result = userValidationStrategy.validateCreation(userRequest, "token");
	        assertFalse(result.isValid());
	        assertEquals("test@example.com is existed.", result.getMessage());
	    }

	    @Test
	    void testValidateCreation_invalidPassword() {
	        when(userService.findByEmail("test@example.com")).thenReturn(null);
	        when(passwordValidatorService.validatePassword("Password123")).thenReturn("Too weak");
	        ValidationResult result = userValidationStrategy.validateCreation(userRequest, "token");
	        assertFalse(result.isValid());
	        assertEquals("Too weak", result.getMessage());
	    }

	    @Test
	    void testValidateCreation_invalidRole() {
	        when(userService.findByEmail("test@example.com")).thenReturn(null);
	        when(passwordValidatorService.validatePassword("Password123")).thenReturn("Valid");
	        when(roleService.findByRoleName("Admin")).thenReturn(null);

	        ValidationResult result = userValidationStrategy.validateCreation(userRequest, "token");
	        assertFalse(result.isValid());
	        assertEquals("Invalid role: Admin", result.getMessage());
	    }


}
