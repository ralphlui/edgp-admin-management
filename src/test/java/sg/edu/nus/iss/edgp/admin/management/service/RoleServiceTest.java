package sg.edu.nus.iss.edgp.admin.management.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import sg.edu.nus.iss.edgp.admin.management.dto.RoleDTO;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;
import sg.edu.nus.iss.edgp.admin.management.repository.RoleRepository;
import sg.edu.nus.iss.edgp.admin.management.service.impl.RoleService;
import sg.edu.nus.iss.edgp.admin.management.utility.DTOMapper;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class RoleServiceTest {

	@InjectMocks
    private RoleService roleService;

    @Mock
    private RoleRepository roleRepository;

    private static Role role1;
    private static Role role2;
    private static List<Role> mockRoles;

    static String authorizationHeader = "Bearer mock.jwt.token";
    static String userId = "user123";

    static RoleDTO mockRoleDTO1 ;
    static RoleDTO mockRoleDTO2 ;
    
    @BeforeEach
    void setUp() {
        role1 = new Role("1", "OrgAdmin", "Organization Admin", true, null, null, null, null);
        role2 = new Role("2", "Analysis", "Data Analysis", true, null, null, null, null);

        mockRoles = new ArrayList<>();
        mockRoles.add(role1);
        mockRoles.add(role2);
        
        mockRoleDTO1 = DTOMapper.toRoleDTO(role1);
        mockRoleDTO2 = DTOMapper.toRoleDTO(role2);
        
    }

    @Test
    void testFindStatusTrue() {
        try (MockedStatic<DTOMapper> mockedMapper = Mockito.mockStatic(DTOMapper.class)) {
            
            when(roleRepository.findByStatusTrue()).thenReturn(mockRoles);

            mockedMapper.when(() -> DTOMapper.toRoleDTO(role1)).thenReturn(mockRoleDTO1);
            mockedMapper.when(() -> DTOMapper.toRoleDTO(role2)).thenReturn(mockRoleDTO2);

            List<RoleDTO> result = roleService.findByStatusTrue();

            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("1", result.get(0).getRoleId());
            assertEquals("2", result.get(1).getRoleId());
            verify(roleRepository, times(1)).findByStatusTrue();
        }
    }
    
    @Test
	void createRole() {
		String userId= "user123";
		role1.setCreatedBy(userId);
		Mockito.when(roleRepository.save(Mockito.any(Role.class))).thenReturn(role1);
		
		Mockito.when(roleRepository.findById(role1.getRoleId())).thenReturn(Optional.of(role1));
		RoleDTO roleDTO = roleService.createRole(role1);
		
		assertEquals(roleDTO.getRoleDescription(), role1.getRoleDescription());
		 
	}

	@Test
	void updateRole() {
		Mockito.when(roleRepository.save(Mockito.any(Role.class))).thenReturn(role1);
		 
		Mockito.when(roleRepository.findById(role1.getRoleId())).thenReturn(Optional.of(role1));
		role1.setRoleDescription("test update");
		RoleDTO roleDTO = roleService.updateRole(role1,authorizationHeader);
		assertEquals(roleDTO.getRoleDescription(), "test update");
	}
	
	@Test
	void findRoleByName() {
		Mockito.when(roleRepository.save(Mockito.any(Role.class))).thenReturn(role1);
		
		Mockito.when(roleRepository.findByRoleName(role1.getRoleName())).thenReturn(role1);
		
		RoleDTO roleDTO = roleService.findByRoleName(role1.getRoleName());
		assertEquals(roleDTO.getRoleId(), role1.getRoleId());
	}

}
