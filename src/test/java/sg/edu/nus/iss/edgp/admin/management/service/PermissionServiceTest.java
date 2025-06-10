package sg.edu.nus.iss.edgp.admin.management.service;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List; 

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import sg.edu.nus.iss.edgp.admin.management.dto.PermissionDTO; 
import sg.edu.nus.iss.edgp.admin.management.entity.Permission;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;
import sg.edu.nus.iss.edgp.admin.management.repository.PermissionRepository;
import sg.edu.nus.iss.edgp.admin.management.repository.RoleRepository;
import sg.edu.nus.iss.edgp.admin.management.service.impl.PermissionService; 
import sg.edu.nus.iss.edgp.admin.management.utility.DTOMapper;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")

class PermissionServiceTest {

	@InjectMocks
	private PermissionService permissionService;
	@Mock
	private PermissionRepository permissionRepository;
	@Mock
	private RoleRepository roleRepository;

	private Permission p1;

	private Permission p2;

	private static List<Permission> permissions;

	@BeforeEach
	void setUp() {
		permissions = new ArrayList<>();
		p1 = new Permission();
		p1.setScope("READ");
		p2 = new Permission();
		p2.setScope("WRITE");
		permissions.add(p1);
		permissions.add(p2);

	}
	@Test
	void testFindPermission_shouldReturnMappedPermissionDTOs() {
		 
		PermissionDTO dto1 =DTOMapper.toPermissionDTO(p1);
	
		PermissionDTO dto2 =DTOMapper.toPermissionDTO(p2);	
		when(permissionRepository.findAll()).thenReturn(permissions);

		try (MockedStatic<DTOMapper> mapper = Mockito.mockStatic(DTOMapper.class)) {
		    mapper.when(() -> DTOMapper.toPermissionDTO(p1)).thenReturn(dto1);
		    mapper.when(() -> DTOMapper.toPermissionDTO(p2)).thenReturn(dto2);

		    List<PermissionDTO> result = permissionService.findPermission();
		
		    assertEquals(2, result.size());
		    assertEquals("READ", result.get(0).getScope());
		    assertEquals("WRITE", result.get(1).getScope());
		    verify(permissionRepository).findAll();
		}

	}

	@Test
	void testFindPermission_shouldThrowException_whenRepositoryFails() {
		when(permissionRepository.findAll()).thenThrow(new RuntimeException("DB error"));

		Exception exception = assertThrows(RuntimeException.class, () -> permissionService.findPermission());

		assertEquals("An error occured while findPermission", exception.getMessage());
		verify(permissionRepository).findAll();
	}

	@Test
	void testFindScopesByRole_shouldReturnScopesIfRoleExists() {
		Role role = new Role();
		role.setRoleId("123");
		List<String> scopes = List.of("read", "write");

		when(roleRepository.findByRoleName("Admin")).thenReturn(role);
		when(permissionRepository.findScopesByRoleId("123")).thenReturn(scopes);

		List<String> result = permissionService.findScopesByRole("Admin");

		assertEquals(scopes, result);
		verify(roleRepository).findByRoleName("Admin");
		verify(permissionRepository).findScopesByRoleId("123");
	}

	@Test
	void testFindScopesByRole_shouldReturnEmptyListIfRoleNotFound() {
		when(roleRepository.findByRoleName("Unknown")).thenReturn(null);

		List<String> result = permissionService.findScopesByRole("Unknown");

		assertTrue(result.isEmpty());
		verify(roleRepository).findByRoleName("Unknown");
	}

	@Test
	void testFindScopesByRole_shouldThrowException_whenRepositoryFails() {
		when(roleRepository.findByRoleName("Admin")).thenThrow(new RuntimeException("DB issue"));

		Exception exception = assertThrows(RuntimeException.class, () -> permissionService.findScopesByRole("Admin"));

		assertEquals("An error occured while findScopesByRole", exception.getMessage());
		verify(roleRepository).findByRoleName("Admin");
	}

}
