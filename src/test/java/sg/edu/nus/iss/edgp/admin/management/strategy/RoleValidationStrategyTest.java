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
public class RoleValidationStrategyTest {
	
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

}
