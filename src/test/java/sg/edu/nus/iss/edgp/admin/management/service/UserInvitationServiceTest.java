package sg.edu.nus.iss.edgp.admin.management.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import sg.edu.nus.iss.edgp.admin.management.dto.UserInvitationDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.UserRequest;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;
import sg.edu.nus.iss.edgp.admin.management.entity.UserInvitation;
import sg.edu.nus.iss.edgp.admin.management.exception.UserNotFoundException;
import sg.edu.nus.iss.edgp.admin.management.repository.RoleRepository;
import sg.edu.nus.iss.edgp.admin.management.repository.UserInvitationRepository;
import sg.edu.nus.iss.edgp.admin.management.repository.UserOrganizationRepository;
import sg.edu.nus.iss.edgp.admin.management.repository.UserRepository;
import sg.edu.nus.iss.edgp.admin.management.service.impl.UserInvitationService;
import sg.edu.nus.iss.edgp.admin.management.utility.DTOMapper;
import sg.edu.nus.iss.edgp.admin.management.utility.JSONReader;

@ExtendWith(MockitoExtension.class)
class UserInvitationServiceTest {

	@Mock
	private UserInvitationRepository userInvitationRepository;

	@Mock
	private RoleRepository roleRepository;

	@Mock
	private JSONReader jsonReader;

	@Mock
	private UserRepository userRepository;

	@Mock
	private UserOrganizationRepository userOrganizationRepository;

	@InjectMocks
	private UserInvitationService userInvitationService;

	static String authorizationHeader = "Bearer mock.jwt.token";
	static String userId = "user123";

	private UserRequest createValidRequest() {
		UserRequest req = new UserRequest();
		req.setEmail("test@gmail.com");
		req.setRole("ADMIN");
		req.setOrganizationId("ORG123");
		return req;
	}

	private Role createRole() {
		Role role = new Role();
		role.setRoleId("R1");
		role.setRoleName("ADMIN");
		return role;
	}

	private UserInvitation createInvitationEntity(UserRequest req, Role role, String token) {
		UserInvitation invitation = new UserInvitation();
		invitation.setEmail(req.getEmail());
		invitation.setOrganizationId(req.getOrganizationId());
		invitation.setRole(role);
		invitation.setToken(token);
		invitation.setUsed(false);
		invitation.setInvitedDate(LocalDateTime.now());
		invitation.setExpiresAt(LocalDateTime.now().plusHours(72));
		return invitation;
	}

	@Test
	void testGenerateSecureToken_LengthAndUniqueness() {
		String token1 = userInvitationService.generateSecureToken();
		String token2 = userInvitationService.generateSecureToken();

		assertNotNull(token1);
		assertNotNull(token2);
		assertEquals(43, token1.length()); // Base64 URL encoded 32 bytes = ~43 chars
		assertNotEquals(token1, token2); // should be unique
	}

	@Test
	void testCreateInvitation_Success() {
		UserRequest req = createValidRequest();
		Role role = createRole();
		UserInvitation savedInvitation = createInvitationEntity(req, role, "mock.jwt.token");

		when(roleRepository.findByRoleName("ADMIN")).thenReturn(role);
		when(userInvitationRepository.save(any(UserInvitation.class))).thenReturn(savedInvitation);

		// Use static mock for DTOMapper
		try (MockedStatic<DTOMapper> mockedMapper = mockStatic(DTOMapper.class)) {
			UserInvitationDTO dto = new UserInvitationDTO();
			dto.setEmail(req.getEmail());

			mockedMapper.when(() -> DTOMapper.toUserInvitationDTO(savedInvitation)).thenReturn(dto);

			UserInvitationDTO result = userInvitationService.createInvitation(req, authorizationHeader);

			assertNotNull(result);
			assertEquals("test@gmail.com", result.getEmail());

			verify(jsonReader).sendUserInviteEmail(savedInvitation, authorizationHeader);
		}
	}

	@Test
	void testCreateInvitation_RoleNotFound() {
		UserRequest req = createValidRequest();

		when(roleRepository.findByRoleName("ADMIN")).thenReturn(null);

		UserInvitationDTO result = userInvitationService.createInvitation(req, authorizationHeader);

		assertNull(result);
		verify(userInvitationRepository, never()).save(any());
		verify(jsonReader, never()).sendUserInviteEmail(any(), any());
	}

	@Test
	void testCreateInvitation_SaveFails() {
		UserRequest req = createValidRequest();
		Role role = createRole();

		when(roleRepository.findByRoleName("ADMIN")).thenReturn(role);
		when(userInvitationRepository.save(any(UserInvitation.class))).thenReturn(null);

		UserInvitationDTO result = userInvitationService.createInvitation(req, authorizationHeader);

		assertNull(result);
		verify(jsonReader, never()).sendUserInviteEmail(any(), any());
	}

	@Test
	void testCreateInvitation_EmailSendThrowsException() {
		UserRequest req = createValidRequest();
		Role role = createRole();
		UserInvitation savedInvitation = createInvitationEntity(req, role, "mock.jwt.token");

		when(roleRepository.findByRoleName("ADMIN")).thenReturn(role);
		when(userInvitationRepository.save(any(UserInvitation.class))).thenReturn(savedInvitation);

		doThrow(new RuntimeException("Email failed")).when(jsonReader).sendUserInviteEmail(any(UserInvitation.class),
				eq(authorizationHeader));

		try (MockedStatic<DTOMapper> mockedMapper = mockStatic(DTOMapper.class)) {
			mockedMapper.when(() -> DTOMapper.toUserInvitationDTO(savedInvitation)).thenReturn(new UserInvitationDTO());

			UserInvitationDTO result = userInvitationService.createInvitation(req, authorizationHeader);

			assertNull(result);
		}
	}

	@Test
	void testExistsByEmailIsUsed_True() {
		when(userInvitationRepository.existsByEmailAndUsedIsTrue("test@gmail.com")).thenReturn(true);

		boolean result = userInvitationService.existsByEmailIsUsed("test@gmail.com");

		assertTrue(result);
	}

	@Test
	void testExistsByEmailIsUsed_ExceptionHandled() {
		when(userInvitationRepository.existsByEmailAndUsedIsTrue("test@gmail.com"))
				.thenThrow(new RuntimeException("Database error"));

		boolean result = userInvitationService.existsByEmailIsUsed("test@gmail.com");

		assertFalse(result); // Should fallback to false
	}

	@Test
	void testFindByTokenAndEmail_Success() {
		UserInvitation invitation = new UserInvitation();
		invitation.setToken("secureToken");
		invitation.setEmail("test@gmail.com");

		when(userInvitationRepository.findByTokenAndEmail("secureToken", "test@gmail.com")).thenReturn(invitation);

		UserInvitation result = userInvitationService.findByTokenAndEmail("secureToken", "test@gmail.com");

		assertNotNull(result);
		assertEquals("secureToken", result.getToken());
		assertEquals("test@gmail.com", result.getEmail());
	}

	@Test
	void testFindByTokenAndEmail_ExceptionHandled() {
		when(userInvitationRepository.findByTokenAndEmail("inv_token", "test@gmail.com"))
				.thenThrow(new RuntimeException("DB error"));

		UserInvitation result = userInvitationService.findByTokenAndEmail("anyToken", "anyEmail");

		assertNull(result);
	}

	@Test
	void testUpdateInvitation_Success() {
		UserRequest req = new UserRequest();
		req.setUserInvitationtoken("secureToken");

		UserInvitation invitation = new UserInvitation();
		invitation.setToken("secureToken");
		invitation.setUsed(false);

		when(userInvitationRepository.findByToken("secureToken")).thenReturn(Optional.of(invitation));
		when(userInvitationRepository.save(any(UserInvitation.class))).thenReturn(invitation);

		try (MockedStatic<DTOMapper> mocked = mockStatic(DTOMapper.class)) {
			UserInvitationDTO dto = new UserInvitationDTO();

			mocked.when(() -> DTOMapper.toUserInvitationDTO(invitation)).thenReturn(dto);

			UserInvitationDTO result = userInvitationService.updateInvitation(req);

			assertNotNull(result);
			assertTrue(invitation.isUsed());
			verify(userInvitationRepository).save(invitation);
		}
	}

	@Test
	void testUpdateInvitation_TokenNotFound_ThrowsException() {
		UserRequest req = new UserRequest();
		req.setUserInvitationtoken("missingToken");

		when(userInvitationRepository.findByToken("missingToken")).thenReturn(Optional.empty());

		assertThrows(UserNotFoundException.class, () -> userInvitationService.updateInvitation(req));
	}

	@Test
	void testUpdateInvitation_SaveFails_ThrowsException() {
		UserRequest req = new UserRequest();
		req.setUserInvitationtoken("token123");

		UserInvitation invitation = new UserInvitation();
		invitation.setToken("token123");

		when(userInvitationRepository.findByToken("token123")).thenReturn(Optional.of(invitation));
		when(userInvitationRepository.save(invitation)).thenThrow(new RuntimeException("Save failed"));

		assertThrows(RuntimeException.class, () -> userInvitationService.updateInvitation(req));
	}

}
