package sg.edu.nus.iss.edgp.admin.management.strategy;

import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
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
    private UserValidationStrategy userValidator;

    private final String VALID_EMAIL = "test@example.com";
    private final String VALID_ROLE = "OrgAdmin";
    private final String VALID_ORG_ID = "ORG123";
    private final String VALID_HEADER = "Bearer token";
    
    private Role role;
    RoleDTO roleDTO ;

    private UserRequest createValidUserRequest() {
        UserRequest req = new UserRequest();
        req.setEmail(VALID_EMAIL);
        req.setUsername("tester");
        req.setPassword("Password@123");
        req.setRole(VALID_ROLE);
        req.setOrganizationId(VALID_ORG_ID);
        req.setUserId("USER123");
        return req;
    }
    @Test
    void testValidateCreation_Success() {
    	 role = new Role("1", "OrgAdmin", "Organization Admin", true, LocalDateTime.now(), null, null, null);
         roleDTO =  DTOMapper.toRoleDTO(role);
        UserRequest req = createValidUserRequest();
        req.setRole(role.getRoleName());

        when(userService.findByEmail(VALID_EMAIL)).thenReturn(null);
        when(passwordValidatorService.validatePassword(req.getPassword())).thenReturn("Valid");
        when(roleService.findByRoleName(VALID_ROLE)).thenReturn(roleDTO);

        ValidationResult result = userValidator.validateCreation(req, VALID_HEADER);

        assertTrue(result.isValid());
        assertNull(result.getMessage());
    }
    @Test
    void testValidateCreation_EmailExists() {
        UserRequest req = createValidUserRequest();
        User existingUser = new User();
        existingUser.setEmail(VALID_EMAIL);
        existingUser.setUserId("EXISTING123");
        existingUser.setUsername("existing");

        when(userService.findByEmail(VALID_EMAIL)).thenReturn(existingUser);

        ValidationResult result = userValidator.validateCreation(req, VALID_HEADER);

        assertFalse(result.isValid());
        assertEquals(VALID_EMAIL + " is existed.", result.getMessage());
    }
    
    @Test
    void testValidateCreation_InvalidPassword() {
        UserRequest req = createValidUserRequest();

        when(userService.findByEmail(VALID_EMAIL)).thenReturn(null);
        when(passwordValidatorService.validatePassword(req.getPassword())).thenReturn("Password too weak");

        ValidationResult result = userValidator.validateCreation(req, VALID_HEADER);

        assertFalse(result.isValid());
        assertEquals("Password too weak", result.getMessage());
    }

    @Test
    void testValidateUpdating_Success() {
    	 role = new Role("1", "OrgAdmin", "Organization Admin", true, LocalDateTime.now(), null, null, null);
         roleDTO =  DTOMapper.toRoleDTO(role);
        UserRequest req = createValidUserRequest();
        User existingUser = new User();
        existingUser.setUserId(req.getUserId());
        existingUser.setUsername("tester");
        existingUser.setActive(true);
        existingUser.setVerified(true);
        existingUser.setRole(role);

        when(userService.findByUserId(req.getUserId())).thenReturn(existingUser);
        when(passwordValidatorService.validatePassword(req.getPassword())).thenReturn("Valid");
        when(roleService.findByRoleName(req.getRole())).thenReturn(roleDTO);

        ValidationResult result = userValidator.validateUpdating(req, VALID_HEADER);

        assertTrue(result.isValid());
    }
    
    @Test
    void testValidateObjectByUserId_UserNotFound() {
        UserRequest req = createValidUserRequest();

        when(userService.findByUserId(req.getUserId())).thenReturn(null);

        ValidationResult result = userValidator.validateObjectByUserId(req, true);

        assertFalse(result.isValid());
        assertEquals("User account not found.", result.getMessage());
    }

    @Test
    void testValidateObject_UserDeleted() {
        User user = new User();
        user.setEmail(VALID_EMAIL);
        user.setUserId("U123");
        user.setUsername("deletedUser");
        user.setActive(false);
        user.setVerified(true);

        when(userService.findByEmail(VALID_EMAIL)).thenReturn(user);

        ValidationResult result = userValidator.validateObject(VALID_EMAIL);

        assertFalse(result.isValid());
        assertEquals("User account is deleted.", result.getMessage());
    }
    @Test
    void testValidateObject_AlreadyInvited() {
        UserRequest req = createValidUserRequest();

        
        when(userService.findByEmail(VALID_EMAIL)).thenReturn(null);
        
        when(userInvitationService.existsByEmailIsUsed(VALID_EMAIL)).thenReturn(true);
        
        ValidationResult result = userValidator.validateObject(req, VALID_HEADER);
         
        assertFalse(result.isValid());
        assertTrue(result.getMessage().contains("Invitation already sent"));
    }

    @Test
    void testValidateObject_InvalidOrganization() {
    	 role = new Role("1", "OrgAdmin", "Organization Admin", true, LocalDateTime.now(), null, null, null);
         roleDTO =  DTOMapper.toRoleDTO(role);
        UserRequest req = createValidUserRequest();

        when(userService.findByEmail(VALID_EMAIL)).thenReturn(null);
        when(userInvitationService.existsByEmailIsUsed(VALID_EMAIL)).thenReturn(false);
        when(roleService.findByRoleName(req.getRole())).thenReturn(roleDTO);
        when(jsonReader.getOrganization(req.getOrganizationId(), VALID_HEADER)).thenReturn(new JSONObject());
        when(jsonReader.getSuccessFromResponse(any(JSONObject.class))).thenReturn(false);

        ValidationResult result = userValidator.validateObject(req, VALID_HEADER);

        assertFalse(result.isValid());
        assertEquals("Organization is not valid.", result.getMessage());
    }
}

