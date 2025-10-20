package sg.edu.nus.iss.edgp.admin.management.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import sg.edu.nus.iss.edgp.admin.management.dto.RoleDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.ValidationResult;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;
import sg.edu.nus.iss.edgp.admin.management.service.impl.RoleService;
import sg.edu.nus.iss.edgp.admin.management.strategy.impl.RoleValidationStrategy;
import sg.edu.nus.iss.edgp.admin.management.utility.DTOMapper;

@ExtendWith(MockitoExtension.class)
class RoleValidationStrategyTest {
	
	@Mock
    private RoleService roleService;

    @InjectMocks
    private RoleValidationStrategy roleValidationStrategy;
 
    
    private Role role;
    RoleDTO roleDTO ;

    private final String jwtToken = "test.jwt.token";
    private final String authorizationHeader = "Bearer " + jwtToken;

    @BeforeEach
    void setup() {
       
        role = new Role("1", "OrgAdmin", "Organization Admin", true, LocalDateTime.now(), null, null, null);
        roleDTO =  DTOMapper.toRoleDTO(role);
    }


    @Test
    void validateCreation_blankRoleName_shouldReturnBadRequest() {
      
        role.setRoleName("");

        ValidationResult result = roleValidationStrategy.validateCreation(role, "mockHeader");

        assertFalse(result.isValid());
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatus());
        assertEquals("Bad Request: Role name could not be blank.", result.getMessage());
    }

    @Test
    void validateCreation_duplicateRole_shouldReturnBadRequest() {
        

        when(roleService.findByRoleName("OrgAdmin")).thenReturn(roleDTO);

        ValidationResult result = roleValidationStrategy.validateCreation(role, authorizationHeader);

        assertFalse(result.isValid());
        assertEquals("Role already exists.", result.getMessage());
    }

    @Test
    void validateCreation_uniqueRole_shouldReturnValidTrue() {
         
        role.setRoleName("NewRole");

       
        when(roleService.findByRoleName("NewRole")).thenReturn(roleDTO);
       
        ValidationResult result = roleValidationStrategy.validateCreation(role, authorizationHeader);

        assertTrue(result.isValid());
        assertNull(result.getMessage());
    }

    @Test
    void validateUpdating_blankRoleName_shouldReturnBadRequest() {
        Role role = new Role();
        role.setRoleName("");

        ValidationResult result = roleValidationStrategy.validateUpdating(role, authorizationHeader);

        assertFalse(result.isValid());
        assertEquals("Bad Request: Role name could not be blank.", result.getMessage());
    }

    @Test
    void validateUpdating_validRole_shouldReturnValidTrue() {
        Role role = new Role();
        role.setRoleName("ValidRole");

        ValidationResult result = roleValidationStrategy.validateUpdating(role, authorizationHeader);

        assertTrue(result.isValid());
        assertNull(result.getMessage());
    }
    
    @Test
    void validateCreation_duplicateRole_caseInsensitive_shouldReturnBadRequest() {
        // role in request is "orgadmin" (different case)
        Role dup = new Role("X", "orgadmin", "desc", true, LocalDateTime.now(), null, null, null);
        // roleService returns existing with different case
        RoleDTO existing = new RoleDTO();
        existing.setRoleId("1");
        existing.setRoleName("OrgAdmin");

        when(roleService.findByRoleName("orgadmin")).thenReturn(existing);

        ValidationResult result = roleValidationStrategy.validateCreation(dup, authorizationHeader);

        assertFalse(result.isValid());
        assertEquals("Role already exists.", result.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, result.getStatus());
    }

    @Test
    void validateCreation_uniqueRole_whenServiceReturnsEmptyDto_shouldBeValid() {
        Role newRole = new Role("N", "BrandNew", "desc", true, LocalDateTime.now(), null, null, null);

        // IMPORTANT: do NOT return null here (would NPE inside strategy). Return an empty DTO.
        RoleDTO empty = new RoleDTO(); // roleId=null, roleName=null
        when(roleService.findByRoleName("BrandNew")).thenReturn(empty);

        ValidationResult result = roleValidationStrategy.validateCreation(newRole, authorizationHeader);

        assertTrue(result.isValid());
        assertNull(result.getMessage());
    }

    @Test
    void validateUpdating_roleIdPresent_butNotFound_returnsBadRequest() {
        Role upd = new Role();
        upd.setRoleId("NOPE");
        upd.setRoleName("SomeName");

        when(roleService.findByRoleId("NOPE")).thenReturn(java.util.Optional.empty());

        ValidationResult res = roleValidationStrategy.validateUpdating(upd, authorizationHeader);

        assertFalse(res.isValid());
        assertEquals("Bad Request: Role Id is invalid.", res.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatus());
    }

    @Test
    void validateUpdating_roleIdPresent_andFound_andNameOk_returnsValidTrue() {
        Role upd = new Role();
        upd.setRoleId("OKID");
        upd.setRoleName("GoodName");

        when(roleService.findByRoleId("OKID")).thenReturn(java.util.Optional.of(
                new Role("OKID", "Anything", "desc", true, LocalDateTime.now(), null, null, null)
        ));

        ValidationResult res = roleValidationStrategy.validateUpdating(upd, authorizationHeader);

        assertTrue(res.isValid());
        assertNull(res.getMessage());
    }
    


}
