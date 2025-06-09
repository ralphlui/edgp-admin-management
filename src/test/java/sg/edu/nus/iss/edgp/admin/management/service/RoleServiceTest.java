package sg.edu.nus.iss.edgp.admin.management.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jsonwebtoken.JwtException;
import sg.edu.nus.iss.edgp.admin.management.dto.RoleDTO;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;
import sg.edu.nus.iss.edgp.admin.management.exception.RoleNotFoundException;
import sg.edu.nus.iss.edgp.admin.management.jwt.JWTService;
import sg.edu.nus.iss.edgp.admin.management.repository.RoleRepository;
import sg.edu.nus.iss.edgp.admin.management.service.impl.RoleService;
import sg.edu.nus.iss.edgp.admin.management.utility.DTOMapper;
import sg.edu.nus.iss.edgp.admin.management.utility.GeneralUtility;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

	@InjectMocks
	private RoleService roleService;

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private JWTService jwtService;

	private static Role role1;
	private static Role role2;
	private static List<Role> mockRoles;

	static String authorizationHeader = "Bearer mock.jwt.token";
	static String userId = "user123";

	static RoleDTO mockRoleDTO1;
	static RoleDTO mockRoleDTO2;

	@BeforeEach
	void setUp() {
		role1 = new Role("1", "OrgAdmin", "Organization Admin", true, LocalDateTime.now(), null, null, null);
		role2 = new Role("2", "Analysis", "Data Analysis", true, LocalDateTime.now(), null, null, null);

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

			assertEquals(2, result.size());
			assertEquals("1", result.get(0).getRoleId());
			assertEquals("2", result.get(1).getRoleId());
			verify(roleRepository, times(1)).findByStatusTrue();
		}
	}

	@Test
	void testFindByStatusTrue_onException() {

		when(roleRepository.findByStatusTrue()).thenThrow(new RuntimeException("DB error"));
		RoleNotFoundException exception = assertThrows(RoleNotFoundException.class, () -> {
			roleService.findByStatusTrue();
		});

		assertEquals("An error occured while findByStatusTrue role", exception.getMessage());
		assertTrue(exception.getCause() instanceof RuntimeException);
		assertEquals("DB error", exception.getCause().getMessage());

		verify(roleRepository).findByStatusTrue();
	}

	@Test
	void testCreate() {

		when(roleRepository.save(any(Role.class))).thenReturn(role1);

		try (MockedStatic<DTOMapper> mocked = mockStatic(DTOMapper.class)) {
			mocked.when(() -> DTOMapper.toRoleDTO(any(Role.class))).thenReturn(mockRoleDTO1);

			RoleDTO roleDTO = roleService.createRole(role1);

			assertEquals("OrgAdmin", roleDTO.getRoleName());
		}
	}

	@Test
	void testCreateRole_onException() {

		when(roleRepository.save(any(Role.class))).thenThrow(new RuntimeException("DB error"));

		RoleDTO result = roleService.createRole(role1);

		assertNull(result);
		verify(roleRepository).save(role1);
	}

	@Test
	void testFindByRoleName_onSuccess() {
		when(roleRepository.findByRoleName("Admin")).thenReturn(role1);

		try (MockedStatic<DTOMapper> mockMapper = Mockito.mockStatic(DTOMapper.class)) {
			mockMapper.when(() -> DTOMapper.toRoleDTO(role1)).thenReturn(mockRoleDTO1);

			RoleDTO result = roleService.findByRoleName("Admin");

			assertNotNull(result);
			assertEquals("OrgAdmin", result.getRoleName());

			verify(roleRepository).findByRoleName("Admin");
			mockMapper.verify(() -> DTOMapper.toRoleDTO(role1));
		}
	}

	@Test
	void testFindByRoleName_onExcetpion() {

		String roleName = "Admin";
		when(roleRepository.findByRoleName(roleName)).thenThrow(new RuntimeException("DB failure"));

		RoleNotFoundException exception = assertThrows(RoleNotFoundException.class, () -> {
			roleService.findByRoleName(roleName);
		});

		assertEquals("An error occured while findByRoleName role", exception.getMessage());
		assertTrue(exception.getCause() instanceof RuntimeException);
		assertEquals("DB failure", exception.getCause().getMessage());

		verify(roleRepository).findByRoleName(roleName);
	}

	@Test
	void testUpdateRole_onSuccess() throws JwtException, IllegalArgumentException, Exception {
		Role dbRole = new Role();
		dbRole.setRoleId("1");
		when(roleRepository.findById("1")).thenReturn(Optional.of(dbRole));
		when(jwtService.getUserIdByAuthHeader(authorizationHeader)).thenReturn("user123");
		when(roleRepository.save(any(Role.class))).thenReturn(dbRole);

		try (MockedStatic<GeneralUtility> generalUtilMock = Mockito.mockStatic(GeneralUtility.class);
				MockedStatic<DTOMapper> dtoMapperMock = Mockito.mockStatic(DTOMapper.class)) {
			generalUtilMock.when(() -> GeneralUtility.makeNotNull("Admin")).thenReturn("Admin");
			generalUtilMock.when(() -> GeneralUtility.makeNotNull("Admin role")).thenReturn("Admin role");
			dtoMapperMock.when(() -> DTOMapper.toRoleDTO(dbRole)).thenReturn(mockRoleDTO1);

			RoleDTO result = roleService.updateRole(role1, authorizationHeader);

			assertNotNull(result);
			assertEquals("OrgAdmin", result.getRoleName());
			verify(roleRepository).save(dbRole);
		}
	}

	@Test
	void testUpdateRole_shouldReturnNull_onException() {
		when(roleRepository.findById("1")).thenThrow(new RuntimeException("DB lookup failed"));

		RoleDTO result = roleService.updateRole(role1, authorizationHeader);

		assertNull(result);
		verify(roleRepository).findById("1");
	}

}
