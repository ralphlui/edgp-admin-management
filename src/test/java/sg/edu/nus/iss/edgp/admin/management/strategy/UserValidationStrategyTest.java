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
    
 // --- helpers ---
    private RoleDTO mkRoleDTO(String id, String name) {
        RoleDTO r = new RoleDTO();
        r.setRoleId(id);
        r.setRoleName(name);
        return r;
    }

    private User mkUser(String id, String email, String name, boolean active, boolean verified) {
        User u = new User();
        u.setUserId(id);
        u.setEmail(email);
        u.setUsername(name);
        u.setActive(active);
        u.setVerified(verified);
        return u;
    }

    // ---------------- validateCreation ----------------

    @Test
    void validateCreation_emailEmpty_returnsBadRequest() {
        UserRequest req = new UserRequest();
        req.setEmail("");            // ← empty
        req.setUsername("who");
        req.setPassword("Password@1");
        req.setRole("OrgAdmin");
        req.setOrganizationId("ORG1");

        ValidationResult res = userValidator.validateCreation(req, VALID_HEADER);
        assertFalse(res.isValid());
        assertEquals("Email cannot be empty.", res.getMessage());
    }

    @Test
    void validateCreation_roleEmpty_returnsBadRequest() {
        UserRequest req = createValidUserRequest();
        req.setRole(""); // empty
        when(userService.findByEmail(VALID_EMAIL)).thenReturn(null);
        when(passwordValidatorService.validatePassword(req.getPassword())).thenReturn("Valid");

        ValidationResult res = userValidator.validateCreation(req, VALID_HEADER);
        assertFalse(res.isValid());
        assertEquals("Role cannot be empty.", res.getMessage());
    }

    @Test
    void validateCreation_invalidRole_returnsBadRequest() {
        UserRequest req = createValidUserRequest();
        when(userService.findByEmail(VALID_EMAIL)).thenReturn(null);
        when(passwordValidatorService.validatePassword(req.getPassword())).thenReturn("Valid");
        when(roleService.findByRoleName(VALID_ROLE)).thenReturn(null); // invalid role

        ValidationResult res = userValidator.validateCreation(req, VALID_HEADER);
        assertFalse(res.isValid());
        assertEquals("Invalid role: " + VALID_ROLE, res.getMessage());
    }

    // ---------------- validateUpdating ----------------
    @Test
    void validateUpdating_roleEmpty_returnsBadRequest() {
        UserRequest req = createValidUserRequest();
        req.setRole("");

        User existing = mkUser(req.getUserId(), req.getEmail(), "tester", true, true);
        when(userService.findByUserId(req.getUserId())).thenReturn(existing);

        
        when(passwordValidatorService.validatePassword(req.getPassword())).thenReturn("Valid");

        ValidationResult res = userValidator.validateUpdating(req, VALID_HEADER);
        assertFalse(res.isValid());
        assertEquals("Role cannot be empty.", res.getMessage());
    }


    @Test
    void validateUpdating_invalidRole_returnsBadRequest() {
        UserRequest req = createValidUserRequest();

        User existing = mkUser(req.getUserId(), req.getEmail(), "tester", true, true);
        when(userService.findByUserId(req.getUserId())).thenReturn(existing);
        when(passwordValidatorService.validatePassword(req.getPassword())).thenReturn("Valid");
        when(roleService.findByRoleName(req.getRole())).thenReturn(mkRoleDTO(null, req.getRole())); // no id

        ValidationResult res = userValidator.validateUpdating(req, VALID_HEADER);
        assertFalse(res.isValid());
        assertEquals("Invalid role: " + req.getRole(), res.getMessage());
    }

    // ------ validateObjectByUserId (user + password paths) ------

    @Test
    void validateObjectByUserId_userInactive_returnsForbidden() {
        UserRequest req = createValidUserRequest();
        User inactive = mkUser(req.getUserId(), req.getEmail(), "x", false, true);
        when(userService.findByUserId(req.getUserId())).thenReturn(inactive);

        ValidationResult res = userValidator.validateObjectByUserId(req, true);
        assertFalse(res.isValid());
        assertEquals("User account is deleted.", res.getMessage());
    }

    @Test
    void validateObjectByUserId_userUnverified_returnsUnauthorized() {
        UserRequest req = createValidUserRequest();
        User unverified = mkUser(req.getUserId(), req.getEmail(), "x", true, false);
        when(userService.findByUserId(req.getUserId())).thenReturn(unverified);

        ValidationResult res = userValidator.validateObjectByUserId(req, true);
        assertFalse(res.isValid());
        assertEquals("Please verify the account first.", res.getMessage());
    }

    @Test
    void validateObjectByUserId_passwordInvalid_returnsBadRequest() {
        UserRequest req = createValidUserRequest();
        User ok = mkUser(req.getUserId(), req.getEmail(), "x", true, true);
        when(userService.findByUserId(req.getUserId())).thenReturn(ok);
        when(passwordValidatorService.validatePassword(req.getPassword())).thenReturn("Too weak");

        ValidationResult res = userValidator.validateObjectByUserId(req, true);
        assertFalse(res.isValid());
        assertEquals("Too weak", res.getMessage());
    }

    @Test
    void validateObjectByUserId_noPasswordValidation_allowedAndValid() {
        UserRequest req = createValidUserRequest();
        User ok = mkUser(req.getUserId(), req.getEmail(), "x", true, true);
        when(userService.findByUserId(req.getUserId())).thenReturn(ok);

        // requiresPasswordValidation=false → should skip passwordValidatorService
        ValidationResult res = userValidator.validateObjectByUserId(req, false);
        assertTrue(res.isValid());
        assertNull(res.getMessage());
    }

    // --------------- validateObject(email) ---------------

    @Test
    void validateObject_emailUserNotFound_returnsNotFound() {
        when(userService.findByEmail(VALID_EMAIL)).thenReturn(null);

        ValidationResult res = userValidator.validateObject(VALID_EMAIL);
        assertFalse(res.isValid());
        assertEquals("User account not found.", res.getMessage());
    }

    // --------------- validateObject(UserRequest, header) ---------------

    @Test
    void validateObject_userReq_emailEmpty_returnsBadRequest() {
        UserRequest req = createValidUserRequest();
        req.setEmail("");

        ValidationResult res = userValidator.validateObject(req, VALID_HEADER);
        assertFalse(res.isValid());
        assertEquals("Email cannot be empty.", res.getMessage());
    }

    @Test
    void validateObject_userReq_emailExists_returnsBadRequest() {
        UserRequest req = createValidUserRequest();
        User exists = mkUser("EX1", req.getEmail(), "exists", true, true);
        when(userService.findByEmail(VALID_EMAIL)).thenReturn(exists);

        ValidationResult res = userValidator.validateObject(req, VALID_HEADER);
        assertFalse(res.isValid());
        assertEquals(VALID_EMAIL + " is existed.", res.getMessage());
    }

    @Test
    void validateObject_userReq_roleEmpty_returnsBadRequest() {
        UserRequest req = createValidUserRequest();
        req.setRole("");
        when(userService.findByEmail(VALID_EMAIL)).thenReturn(null);
        when(userInvitationService.existsByEmailIsUsed(VALID_EMAIL)).thenReturn(false);

        ValidationResult res = userValidator.validateObject(req, VALID_HEADER);
        assertFalse(res.isValid());
        assertEquals("Role cannot be empty.", res.getMessage());
    }

    @Test
    void validateObject_userReq_invalidRole_returnsBadRequest() {
        UserRequest req = createValidUserRequest();
        when(userService.findByEmail(VALID_EMAIL)).thenReturn(null);
        when(userInvitationService.existsByEmailIsUsed(VALID_EMAIL)).thenReturn(false);
        when(roleService.findByRoleName(VALID_ROLE)).thenReturn(mkRoleDTO(null, VALID_ROLE)); // invalid id

        ValidationResult res = userValidator.validateObject(req, VALID_HEADER);
        assertFalse(res.isValid());
        assertEquals("Invalid role: " + VALID_ROLE, res.getMessage());
    }

    @Test
    void validateObject_userReq_orgEmpty_returnsBadRequest() {
        UserRequest req = createValidUserRequest();
        req.setOrganizationId(null);

        when(userService.findByEmail(VALID_EMAIL)).thenReturn(null);
        when(userInvitationService.existsByEmailIsUsed(VALID_EMAIL)).thenReturn(false);
        when(roleService.findByRoleName(VALID_ROLE)).thenReturn(mkRoleDTO("1", VALID_ROLE));

        ValidationResult res = userValidator.validateObject(req, VALID_HEADER);
        assertFalse(res.isValid());
        assertEquals("Organization cannot be empty.", res.getMessage());
    }

    @Test
    void validateObject_userReq_success_validTrue() {
        UserRequest req = createValidUserRequest();

        when(userService.findByEmail(VALID_EMAIL)).thenReturn(null);
        when(userInvitationService.existsByEmailIsUsed(VALID_EMAIL)).thenReturn(false);
        when(roleService.findByRoleName(VALID_ROLE)).thenReturn(mkRoleDTO("1", VALID_ROLE));
        // org present + header present → JSONReader consulted and success=true
        JSONObject orgResp = new JSONObject();
        when(jsonReader.getOrganization(VALID_ORG_ID, VALID_HEADER)).thenReturn(orgResp);
        when(jsonReader.getSuccessFromResponse(orgResp)).thenReturn(true);

        ValidationResult res = userValidator.validateObject(req, VALID_HEADER);
        assertTrue(res.isValid());
        assertNull(res.getMessage());
    }

}

