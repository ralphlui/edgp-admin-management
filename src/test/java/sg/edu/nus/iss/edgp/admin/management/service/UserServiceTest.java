package sg.edu.nus.iss.edgp.admin.management.service;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import io.jsonwebtoken.JwtException;
import sg.edu.nus.iss.edgp.admin.management.configuration.AWSConfig;
import sg.edu.nus.iss.edgp.admin.management.dto.RoleDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.UserDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.UserRequest;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;
import sg.edu.nus.iss.edgp.admin.management.entity.User;
import sg.edu.nus.iss.edgp.admin.management.entity.UserInvitation;
import sg.edu.nus.iss.edgp.admin.management.entity.UserOrganization;
import sg.edu.nus.iss.edgp.admin.management.exception.UserNotFoundException;
import sg.edu.nus.iss.edgp.admin.management.jwt.JWTService;
import sg.edu.nus.iss.edgp.admin.management.repository.RoleRepository;
import sg.edu.nus.iss.edgp.admin.management.repository.UserInvitationRepository;
import sg.edu.nus.iss.edgp.admin.management.repository.UserOrganizationRepository;
import sg.edu.nus.iss.edgp.admin.management.repository.UserRepository;
import sg.edu.nus.iss.edgp.admin.management.service.impl.UserService;
import sg.edu.nus.iss.edgp.admin.management.utility.DTOMapper;
import sg.edu.nus.iss.edgp.admin.management.utility.EncryptionUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	private static List<User> mockUsers = new ArrayList<>();

	@InjectMocks
	private UserService userService;

	@Mock
	private UserRepository userRepository;

	@Mock
	private RoleRepository roleRepository;
	
	@Mock
	private UserInvitationRepository userInvitationRepository;
	
	@Mock
	private UserOrganizationRepository userOrganizationRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private EncryptionUtils encryptionUtils;

	@Mock
	private AWSConfig awsConfig;
	
	@Mock
	private JWTService jwtService;

	@Value("${client.url}")
	private String frontEndUrl = "https://example.com";

	static String authorizationHeader = "Bearer mock.jwt.token";
	static String userId = "user123";

	private static User user1;
	private static User user2;

	private static UserRequest userRequest;

	private final String encryptedCode = "encryptedCode123";
	private final String decryptedCode = "decryptedCode456";

	static UserDTO mockUserDTO1;
	static UserDTO mockUserDTO2;

	private static Role role1;

	private static Role role2;

	@BeforeEach
	void setUp() {

		role1 = new Role("1", "OrgAdmin", "Organization Admin", true, null, null, null, null);
		userRequest = new UserRequest();
		userRequest.setUserId("user123");
	    userRequest.setUsername("John");
	    userRequest.setPassword("encryptedCode123");
	    userRequest.setRole("OrgAdmin");
	    userRequest.setActive(true);

		user1 = new User();
		user1.setUserId("user123");
		user1.setUsername(userRequest.getUsername());
		user1.setRole(role1);
		user1.setPassword(userRequest.getPassword());
		user1.setActive(true);

		mockUserDTO1 = DTOMapper.toUserDTO(user1);

		role2 = new Role("2", "Analysis", "Data Analysis", true, LocalDateTime.now(), null, null, null);
		user2 = new User();
		user2.setUsername("Emma");
		user2.setRole(role2);
		user2.setPassword("see!12dS");
		user2.setActive(true);
		mockUserDTO2 = DTOMapper.toUserDTO(user2);

		mockUsers.add(user1);
		mockUsers.add(user2);

	}
 

	@Test
	void testCreateUser_onSuccess() {
		when(passwordEncoder.encode("encryptedCode123")).thenReturn(encryptedCode);
		when(roleRepository.findByRoleName("OrgAdmin")).thenReturn(role1);
		when(userRepository.save(any(User.class))).thenReturn(user1);

		try (MockedStatic<DTOMapper> mockMapper = Mockito.mockStatic(DTOMapper.class)) {
			mockMapper.when(() -> DTOMapper.toUserDTO(user1)).thenReturn(mockUserDTO1);

			UserDTO result = userService.createUser(userRequest);

			assertNotNull(result);
			assertEquals("John", result.getUsername());

			verify(roleRepository).findByRoleName("OrgAdmin");
			verify(passwordEncoder).encode("encryptedCode123");
			verify(userRepository).save(any(User.class));
			mockMapper.verify(() -> DTOMapper.toUserDTO(user1));
		}
	}
	

	@Test
	void testCreateUser_onException() {
		when(roleRepository.findByRoleName("OrgAdmin")).thenThrow(new RuntimeException("DB error"));

		UserDTO result = userService.createUser(userRequest);

		assertNull(result);
		verify(roleRepository).findByRoleName("OrgAdmin");
	}
	
	
	@Test
	void testUpdateUser_onSuccess() {

	    User updateUser = new User();
	    updateUser.setUsername("John");
	    updateUser.setRole(role1);
	    updateUser.setPassword("encodedPassword");
	    updateUser.setActive(true);

	    UserDTO updatedDTO = DTOMapper.toUserDTO(updateUser);

	    when(userRepository.findByUserId("user123")).thenReturn(user1);
	    when(passwordEncoder.encode("encryptedCode123")).thenReturn("encodedPassword");
	    when(roleRepository.findByRoleName("OrgAdmin")).thenReturn(role1);
	    when(userRepository.save(any(User.class))).thenReturn(updateUser);

	    try (MockedStatic<DTOMapper> mapperMock = Mockito.mockStatic(DTOMapper.class)) {
	        mapperMock.when(() -> DTOMapper.toUserDTO(updateUser)).thenReturn(updatedDTO);

	        UserDTO result = userService.updateUser(userRequest);
	        assertNotNull(result);
	        assertEquals("John", result.getUsername());

	        verify(userRepository).findByUserId("user123");
	        verify(userRepository).save(any(User.class));
	    }
	}


	@Test
	void testUpdateUser_UserNotFound() {
	    userRequest = new UserRequest();
	    userRequest.setUserId("user123");

	    when(userRepository.findByUserId("user123")).thenReturn(null);

	    assertThrows(UserNotFoundException.class, () -> {
	        userService.updateUser(userRequest);
	    });

	    verify(userRepository).findByUserId("user123");
	}

    @Test
    void testUpdateUser_onThrowException() {
        when(userRepository.findByUserId("user123")).thenReturn(user1);
        when(roleRepository.findByRoleName("OrgAdmin")).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> {
            userService.updateUser(userRequest);
        });

        verify(userRepository).findByUserId("user123");
        verify(roleRepository).findByRoleName("OrgAdmin");
    }

    @Test
    void findActiveUsers() {
        Pageable pageable = PageRequest.of(0, 10);
 
        List<User> userList = Collections.singletonList(user1);

        Page<User> userPage = new PageImpl<>(userList, pageable, userList.size());

        when(userRepository.findByIsActiveAndIsVerified(true, true, pageable)).thenReturn(userPage);

        UserDTO userDTO = new UserDTO();
        userDTO.setEmail("john@gmail.com");

        try (MockedStatic<DTOMapper> mapperMock = Mockito.mockStatic(DTOMapper.class)) {
            mapperMock.when(() -> DTOMapper.toUserDTO(user1)).thenReturn(userDTO);

            Map<Long, List<UserDTO>> result = userService.findActiveUsers(pageable);

            assertNotNull(result);
            assertTrue(result.containsKey(1L));
            assertEquals(1, result.get(1L).size());
            assertEquals("john@gmail.com", result.get(1L).get(0).getEmail());
            verify(userRepository).findByIsActiveAndIsVerified(true, true, pageable);
        }
    }
    
    @Test
    void findActiveUsers_onNoUsers() {
    	 Pageable pageable = PageRequest.of(0, 10);

        Page<User> emptyPage = new PageImpl<>(Collections.emptyList());
        when(userRepository.findByIsActiveAndIsVerified(true, true, pageable)).thenReturn(emptyPage);
 
        Map<Long, List<UserDTO>> result = userService.findActiveUsers(pageable);
 
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.values().iterator().next().isEmpty());
    }
    
    @Test
    void findActiveUsers_onThrowException() {
    	 Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.findByIsActiveAndIsVerified(true, true, pageable))
                .thenThrow(new RuntimeException("DB error"));
 
        assertThrows(RuntimeException.class, () -> userService.findActiveUsers(pageable));
    }
    
    @Test
    void testVerifyUser_onSuccess() throws Exception {
        String rawCode = "sample-code";
        String decryptedCode = "decrypted-code";

        User user = new User();
        user.setVerified(false);

        UserDTO expectedDTO = new UserDTO();
        expectedDTO.setUsername("John");

        when(encryptionUtils.decrypt(rawCode)).thenReturn(decryptedCode);
        when(userRepository.findByVerificationCodeAndIsActiveAndIsVerified(decryptedCode, false, true)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);

        try (MockedStatic<DTOMapper> mapperMock = Mockito.mockStatic(DTOMapper.class)) {
            mapperMock.when(() -> DTOMapper.toUserDTO(user)).thenReturn(expectedDTO);

            UserDTO result = userService.verifyUser(rawCode);

            assertNotNull(result);
            assertEquals("John", result.getUsername());

            verify(userRepository).findByVerificationCodeAndIsActiveAndIsVerified(decryptedCode, false, true);
            verify(userRepository).save(user);
            mapperMock.verify(() -> DTOMapper.toUserDTO(user));
        }
    }
    
    @Test
    void testVerifyUser_onUserNotFound() throws Exception {
        String rawCode = "sample-code";
        String decryptedCode = "decrypted-code";

        when(encryptionUtils.decrypt(rawCode)).thenReturn(decryptedCode);
        when(userRepository.findByVerificationCodeAndIsActiveAndIsVerified(decryptedCode, false, true)).thenReturn(null);

        UserDTO result = userService.verifyUser(rawCode);

        assertNull(result);
        verify(userRepository).findByVerificationCodeAndIsActiveAndIsVerified(decryptedCode, false, true);
    }
    
    @Test
    void testFindActiveUserByID() {
        String userId = "user123";
        User activeUser = new User();
        activeUser.setUserId(userId);
        activeUser.setActive(true);
        activeUser.setVerified(true);

        when(userRepository.findByUserIdAndIsActiveAndIsVerified(userId, true, true)).thenReturn(activeUser);

        User result = userService.findActiveUserByID(userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        verify(userRepository).findByUserIdAndIsActiveAndIsVerified(userId, true, true);
    }
    
    @Test
    void testFindActiveUserByID_onThrowException() {
        String userId = "user123";

        when(userRepository.findByUserIdAndIsActiveAndIsVerified(userId, true, true)).thenReturn(null);

        assertThrows(UserNotFoundException.class, () -> {
            userService.findActiveUserByID(userId);
        });

        verify(userRepository).findByUserIdAndIsActiveAndIsVerified(userId, true, true);
    }
    
    @Test
    void testFindByUserId_onThrowException() {
        String userId = "user123";

        when(userRepository.findByUserId(userId))
            .thenThrow(new RuntimeException("Database error"));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            userService.findByUserId(userId);
        });

        assertEquals("Database error", thrown.getMessage());
        verify(userRepository).findByUserId(userId);
    }


    @Test
    void testLoginUser_onSuccess() {
        String email = "test@example.com";
        String password = "password123";
        String encodedPassword = "encodedPassword123";

        User mockUser = new User();
        mockUser.setEmail(email);
        mockUser.setPassword(encodedPassword);
        mockUser.setActive(true);
        mockUser.setVerified(true);

        UserDTO mockUserDTO = new UserDTO();
        mockUserDTO.setEmail(email);

        when(userRepository.findByEmailAndIsActiveAndIsVerified(email, true, true)).thenReturn(mockUser);
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);

        try (MockedStatic<DTOMapper> mapperMock = Mockito.mockStatic(DTOMapper.class)) {
            mapperMock.when(() -> DTOMapper.toUserDTO(mockUser)).thenReturn(mockUserDTO);

            UserDTO result = userService.loginUser(email, password);

            assertNotNull(result);
            assertEquals(email, result.getEmail());
            verify(userRepository).findByEmailAndIsActiveAndIsVerified(email, true, true);
            verify(passwordEncoder).matches(password, encodedPassword);
            mapperMock.verify(() -> DTOMapper.toUserDTO(mockUser));
        }
    }

    @Test
    void testLoginUser_onPasswordDoesNotMatch() {
        String email = "test@example.com";
        String password = "wrongPassword";

        User mockUser = new User();
        mockUser.setEmail(email);
        mockUser.setPassword("encodedPassword123");
        mockUser.setActive(true);
        mockUser.setVerified(true);

        when(userRepository.findByEmailAndIsActiveAndIsVerified(email, true, true)).thenReturn(mockUser);
        when(passwordEncoder.matches(password, "encodedPassword123")).thenReturn(false);

        assertThrows(UserNotFoundException.class, () -> {
            userService.loginUser(email, password);
        });

        verify(userRepository).findByEmailAndIsActiveAndIsVerified(email, true, true);
        verify(passwordEncoder).matches(password, "encodedPassword123");
    }

    @Test
    void testLoginUser_onUserIsNotFound() {
        String email = "test@example.com";
        String password = "password123";

        when(userRepository.findByEmailAndIsActiveAndIsVerified(email, true, true)).thenReturn(null);

        assertThrows(UserNotFoundException.class, () -> {
            userService.loginUser(email, password);
        });

        verify(userRepository).findByEmailAndIsActiveAndIsVerified(email, true, true);
    }
    @Test
    void testCheckSpecificActiveUserByID() {
        String userId = "user123";
        User mockUser = new User();
        mockUser.setUserId(userId);
        mockUser.setActive(true);
        mockUser.setVerified(true);

        UserDTO mockUserDTO = new UserDTO();
        mockUserDTO.setEmail("john@gmail.com");

        // Mocking internal method call
        when(userRepository.findByUserIdAndIsActiveAndIsVerified(userId, true, true)).thenReturn(mockUser);

        try (MockedStatic<DTOMapper> mapperMock = Mockito.mockStatic(DTOMapper.class)) {
            mapperMock.when(() -> DTOMapper.toUserDTO(mockUser)).thenReturn(mockUserDTO);

            UserDTO result = userService.checkSpecificActiveUserByID(userId);

            assertNotNull(result);
            assertEquals("john@gmail.com", result.getEmail());

            mapperMock.verify(() -> DTOMapper.toUserDTO(mockUser));
            verify(userRepository).findByUserIdAndIsActiveAndIsVerified(userId, true, true);
        }
    }
    
    @Test
    void testCheckSpecificActiveUserByID_onUserNotFound() {
        String userId = "user123";

        when(userRepository.findByUserIdAndIsActiveAndIsVerified(userId, true, true)).thenReturn(null);

        assertThrows(UserNotFoundException.class, () -> {
            userService.checkSpecificActiveUserByID(userId);
        });

        verify(userRepository).findByUserIdAndIsActiveAndIsVerified(userId, true, true);
    }

    
    @Test
    void testCheckSpecificActiveUserByEmail_onUserExists() {
        String email = "john@example.com";
        User mockUser = new User();
        mockUser.setEmail(email);
        mockUser.setActive(true);
        mockUser.setVerified(true);

        UserDTO mockUserDTO = new UserDTO();
        mockUserDTO.setEmail(email);

        when(userRepository.findByEmailAndIsActiveAndIsVerified(email, true, true)).thenReturn(mockUser);

        try (MockedStatic<DTOMapper> mapperMock = Mockito.mockStatic(DTOMapper.class)) {
            mapperMock.when(() -> DTOMapper.toUserDTO(mockUser)).thenReturn(mockUserDTO);

            UserDTO result = userService.checkSpecificActiveUserByEmail(email);

            assertNotNull(result);
            assertEquals(email, result.getEmail());

            verify(userRepository).findByEmailAndIsActiveAndIsVerified(email, true, true);
            mapperMock.verify(() -> DTOMapper.toUserDTO(mockUser));
        }
    }
    
    @Test
    void testCheckSpecificActiveUserByEmail_onNotFound() {
        String email = "nonexistent@example.com";

        when(userRepository.findByEmailAndIsActiveAndIsVerified(email, true, true)).thenReturn(null);

        assertThrows(UserNotFoundException.class, () -> {
            userService.checkSpecificActiveUserByEmail(email);
        });

        verify(userRepository).findByEmailAndIsActiveAndIsVerified(email, true, true);
    }

    @Test
    void testAccountActivate_onSuccess() throws JwtException, IllegalArgumentException, Exception {
        UserRequest userReq = new UserRequest();
        userReq.setUsername("john");
        userReq.setPassword("password123");
        userReq.setUserInvitationtoken("invitationToken");

        UserInvitation userInvitation = new UserInvitation();
        userInvitation.setEmail("john@example.com");
        userInvitation.setOrganizationId("121112211");
        Role role = new Role();
        role.setRoleName("OrgAdmin");
        userInvitation.setRole(role);

        User createdUser = new User();
        createdUser.setUsername("john");

        UserOrganization createdUserOrg = new UserOrganization();

        UserDTO mockUserDTO = new UserDTO();
        mockUserDTO.setUsername("john");

        when(userInvitationRepository.findByToken("invitationToken")).thenReturn(Optional.of(userInvitation));
        when(jwtService.getUserIdByAuthHeader(authorizationHeader)).thenReturn("adminUserId");
        when(roleRepository.findByRoleName("OrgAdmin")).thenReturn(role);
        when(userRepository.save(any(User.class))).thenReturn(createdUser);
        when(userOrganizationRepository.save(any(UserOrganization.class))).thenReturn(createdUserOrg);

        try (MockedStatic<DTOMapper> mapperMock = Mockito.mockStatic(DTOMapper.class)) {
            mapperMock.when(() -> DTOMapper.toUserDTO(createdUser)).thenReturn(mockUserDTO);

            UserDTO result = userService.accountActivate(userReq,authorizationHeader);

            assertNotNull(result);
            assertEquals("john", result.getUsername());

            verify(userInvitationRepository).findByToken("invitationToken");
            verify(userRepository).save(any(User.class));
            verify(userOrganizationRepository).save(any(UserOrganization.class));
            verify(roleRepository).findByRoleName("OrgAdmin");
        }
    }
    @Test
    void testAccountActivate_onInvitationNotFound() {
        UserRequest userReq = new UserRequest();
        userReq.setUserInvitationtoken("invalidToken");

        when(userInvitationRepository.findByToken("invalidToken")).thenReturn(Optional.empty());

        UserDTO result = userService.accountActivate(userReq, "Bearer token");

        assertNull(result);
        verify(userInvitationRepository).findByToken("invalidToken");
    }
    @Test
    void testAccountActivate_onException() throws JwtException, IllegalArgumentException, Exception {
        UserRequest userReq = new UserRequest();
        userReq.setUserInvitationtoken("validToken");

        UserInvitation userInvitation = new UserInvitation();
        Role role = new Role();
        role.setRoleName("OrgAdmin");
        userInvitation.setRole(role);

        when(userInvitationRepository.findByToken("validToken")).thenReturn(Optional.of(userInvitation));
        when(jwtService.getUserIdByAuthHeader("Bearer token")).thenReturn("adminUserId");
 
        when(roleRepository.findByRoleName("OrgAdmin")).thenThrow(new RuntimeException("Simulated failure"));

        UserDTO result = userService.accountActivate(userReq, "Bearer token");

        assertNull(result);
        verify(userInvitationRepository).findByToken("validToken");
        verify(jwtService).getUserIdByAuthHeader("Bearer token");
        verify(roleRepository).findByRoleName("OrgAdmin");
    }

    @Test
    void testResetPassword_success() {
        String email = "test@example.com";
        String rawPassword = "newPassword";
        String encodedPassword = "encodedPassword";

        User mockUser = new User();
        mockUser.setEmail(email);
        mockUser.setPassword("oldPassword");

        User updatedUser = new User();
        updatedUser.setEmail(email);
        updatedUser.setPassword(encodedPassword);

        UserDTO expectedDTO = new UserDTO();
        expectedDTO.setEmail(email);

        when(userRepository.findByEmailAndIsActiveAndIsVerified(email, true, true)).thenReturn(mockUser);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(mockUser)).thenReturn(updatedUser);

        try (MockedStatic<DTOMapper> mockMapper = Mockito.mockStatic(DTOMapper.class)) {
            mockMapper.when(() -> DTOMapper.toUserDTO(updatedUser)).thenReturn(expectedDTO);

            UserDTO result = userService.resetPassword(email, rawPassword);

            assertNotNull(result);
            assertEquals(email, result.getEmail());

            verify(userRepository).findByEmailAndIsActiveAndIsVerified(email, true, true);
            verify(passwordEncoder).encode(rawPassword);
            verify(userRepository).save(mockUser);
            mockMapper.verify(() -> DTOMapper.toUserDTO(updatedUser));
        }
    }
    
    @Test
    void testResetPassword_onException() {
        String email = "nonexistent@example.com";

        when(userRepository.findByEmailAndIsActiveAndIsVerified(email, true, true)).thenReturn(null);

        UserNotFoundException thrown = assertThrows(UserNotFoundException.class, () ->
            userService.resetPassword(email, "anyPassword")
        );

        assertTrue(thrown.getMessage().contains("Unable to find the user with this email"));

        verify(userRepository).findByEmailAndIsActiveAndIsVerified(email, true, true);
    }
    @Test
    void testFindByEmail() {
        String email = "test@example.com";
        User mockUser = new User();
        mockUser.setEmail(email);

        when(userRepository.findByEmail(email)).thenReturn(mockUser);

        User result = userService.findByEmail(email);

        assertNotNull(result);
        assertEquals(email, result.getEmail());
        verify(userRepository).findByEmail(email);
    }
    
    @Test
    void testRetrieveUserIDAndNameFromToken() throws Exception {
        String token = "Bearer mock.jwt.token";
        String expectedUserId = "user123";
        String expectedUsername = "john_doe";

        when(jwtService.extractUserIdAllowExpiredToken("mock.jwt.token")).thenReturn(expectedUserId);
        when(jwtService.extractUserNameAllowExpiredToken("mock.jwt.token")).thenReturn(expectedUsername);

        HashMap<String, String> result = userService.retrieveUserIDAndNameFromToken(token);

        assertNotNull(result);
        assertEquals(expectedUserId, result.get("INVALID_USER_ID"));
        assertEquals(expectedUsername, result.get("INVALID_USER_NAME"));

        verify(jwtService).extractUserIdAllowExpiredToken("mock.jwt.token");
        verify(jwtService).extractUserNameAllowExpiredToken("mock.jwt.token");
    }  


}
