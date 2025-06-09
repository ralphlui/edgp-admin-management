package sg.edu.nus.iss.edgp.admin.management.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import sg.edu.nus.iss.edgp.admin.management.configuration.JWTConfig;
import sg.edu.nus.iss.edgp.admin.management.dto.UserDTO;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;
import sg.edu.nus.iss.edgp.admin.management.entity.User;
import sg.edu.nus.iss.edgp.admin.management.entity.UserOrganization;
import sg.edu.nus.iss.edgp.admin.management.repository.UserInvitationRepository;
import sg.edu.nus.iss.edgp.admin.management.repository.UserOrganizationRepository;
import sg.edu.nus.iss.edgp.admin.management.repository.UserRepository;
import sg.edu.nus.iss.edgp.admin.management.service.impl.PermissionService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.UserService;
import sg.edu.nus.iss.edgp.admin.management.utility.DTOMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import java.lang.reflect.Field;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class JWTServiceTest {

	@InjectMocks
	private JWTService jwtService;
	
	@Mock
	private JWTConfig jwtConfig;
	
	@Mock
	private ApplicationContext context;
	
	@Mock
	private PermissionService permissionService;
	
	@Mock
	private  UserOrganizationRepository userOrganizationRepository;
	
	@Mock
	private  UserInvitationRepository userInvitationRepository;

	private KeyPair keyPair;

	@InjectMocks
	private UserService userService;

	@Mock
	private UserRepository userRepository;
	
	
	private static User user;
	private static Role role;
	private static UserDTO mockUserDTO;
	
	
	// Control flag per test
	static ThreadLocal<String> pentestValue = ThreadLocal.withInitial(() -> "true");

	@DynamicPropertySource
	static void dynamicProperties(DynamicPropertyRegistry registry) {
		registry.add("pentest.enable", () -> pentestValue.get());
	}

	@BeforeEach
	public void setup() throws Exception {
		role = new Role("1", "OrgAdmin", "Organization Admin", true, null, null, null, null);
		user = new User();
		user.setUserId("user123");
		user.setUsername("John");
		user.setEmail("john@gmail.com");
		user.setRole(role);
		user.setPassword("122@ddDs");
		user.setActive(true);

		mockUserDTO = DTOMapper.toUserDTO(user);
		
		
		// Generate RSA key pair for testing

		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048);
		keyPair = keyGen.generateKeyPair();

		jwtConfig = mock(JWTConfig.class);
		context = mock(ApplicationContext.class);

		when(jwtConfig.getJWTPrivateKey())
				.thenReturn(Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
		when(jwtConfig.getJWTPubliceKey())
				.thenReturn(Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));

		jwtService = new JWTService(jwtConfig, context,permissionService,userOrganizationRepository,userInvitationRepository);
		Field field = JWTService.class.getDeclaredField("pentestEnable");
		field.setAccessible(true);
		field.set(jwtService, "true");
	}

	
	@Test
	public void testExtractUserId() throws Exception {
		String token = jwtService.generateToken(mockUserDTO);
		String userId = jwtService.extractUserID(token);
		assertEquals("user123", userId);
	}

	@Test
	public void testExtractUsername() throws Exception {
		String token = jwtService.generateToken(mockUserDTO);
		Claims claims = jwtService.extractAllClaims(token);
		assertEquals("John", claims.get(JWTService.CLAIM_USERNAME));
	}

	@Test
	public void testTokenExpiration() throws Exception {
		 
        List<String> scopes = Arrays.asList("org:create", "user:invite");
        when(permissionService.findScopesByRole("OrgAdmin")).thenReturn(scopes);
        when(userInvitationRepository.existsByEmail("john@gmail.com")).thenReturn(true);

        UserOrganization userOrg = new UserOrganization();
        userOrg.setRole(role);
        userOrg.setUser(user);
        userOrg.setOrganizationId("org789");

        when(userOrganizationRepository.findByUser_UserIdAndRole_RoleId("user123", "1"))
            .thenReturn(userOrg);

       
        String token = jwtService.generateToken(mockUserDTO);
        assertNotNull(token);
		
		
		
		Date expiration = jwtService.extractExpiration(token);
		assertTrue(expiration.after(new Date()));
	}

	@Test
	public void testExtractUserIdFromExpiredToken() throws JwtException, IllegalArgumentException, Exception {
		// Generate an expired token
		Date now = new Date();
		Date issuedAt = new Date(now.getTime() - 60 * 60 * 1000); // 1 hour ago
		Date expiration = new Date(now.getTime() - 30 * 60 * 1000); // 30 mins ago

		String expiredToken = Jwts.builder().subject("expiredUser").claim(JWTService.CLAIM_USERNAME, "ExpiredUser")
				.issuedAt(issuedAt).expiration(expiration).signWith(keyPair.getPrivate()).compact();

		String userId = jwtService.extractUserIdAllowExpiredToken(expiredToken);
		assertEquals("expiredUser", userId);
	}
	
	

    @Test
    void testExtractUserNameFromExpiredToken() throws Exception {
        // Simulate ExpiredJwtException
    	Date now = new Date();
		Date issuedAt = new Date(now.getTime() - 60 * 60 * 1000); // 1 hour ago
		Date expiration = new Date(now.getTime() - 30 * 60 * 1000); // 30 mins ago

		String expiredToken = Jwts.builder().subject("expiredUser").claim(JWTService.CLAIM_USERNAME, "ExpiredUser")
				.issuedAt(issuedAt).expiration(expiration).signWith(keyPair.getPrivate()).compact();

		String userId = jwtService.extractUserNameAllowExpiredToken(expiredToken);
		assertEquals("ExpiredUser", userId);
    }
}