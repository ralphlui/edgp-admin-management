package sg.edu.nus.iss.edgp.admin.management.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import sg.edu.nus.iss.edgp.admin.management.entity.RefreshToken;
import sg.edu.nus.iss.edgp.admin.management.entity.User;
import sg.edu.nus.iss.edgp.admin.management.exception.UserNotFoundException;
import sg.edu.nus.iss.edgp.admin.management.jwt.JWTService;
import sg.edu.nus.iss.edgp.admin.management.repository.RefreshTokenRepository;
import sg.edu.nus.iss.edgp.admin.management.service.impl.RefreshTokenService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.UserService;
import sg.edu.nus.iss.edgp.admin.management.utility.GeneralUtility;


@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")

 class RefreshTokenServiceTest {

	@Mock
	private UserService userService;

	@Mock
	private JWTService jwtService;

	@Mock
	private RefreshTokenRepository refreshTokenRepository;

	@InjectMocks
	private RefreshTokenService refreshTokenService;



	private String userId;
	private String token;
	private String hashedToken;
	private User user;
	private Date expiryDate;
	private LocalDateTime localExpiryDateTime;
	private Boolean revoked;
	private static final String PLAIN_TOKEN = "rawToken123";
	private static final String HASHED_TOKEN = "hashedToken456";


	@BeforeEach
	void setUp() throws Exception {
		refreshTokenService = new RefreshTokenService(userService, refreshTokenRepository);
		userId = "user123";
		token = "sample.jwt.token";
		hashedToken = "hashedSampleToken";
		user = new User();
		user.setUserId(userId);

		expiryDate = new Date(System.currentTimeMillis() + 1000 * 60 * 60); // 1 hour expiry
		localExpiryDateTime = expiryDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
		revoked = true;
	}
	
	 

	@Test
	void testUpdateRefreshTokenSuccess() {
 
		mockStatic(GeneralUtility.class);
		when(GeneralUtility.hashWithSHA256(token)).thenReturn(hashedToken);

		 
		assertDoesNotThrow(() -> refreshTokenService.updateRefreshToken(token, revoked));

	}

	@Test
	void testUpdateRefreshTokenException() {
		
		String rawToken = "sample_token";

		when(GeneralUtility.hashWithSHA256(rawToken)).thenThrow(new RuntimeException("Hashing failed"));

		RuntimeException exception = assertThrows(RuntimeException.class, () -> {
			refreshTokenService.updateRefreshToken(rawToken, true);
		});

		assertEquals("Error updating refresh token", exception.getMessage());
		verify(refreshTokenRepository, never()).updateRefreshToken(anyBoolean(), any(), any());
	}
	
	@Test
	void testSaveRefreshToken_Success() throws Exception {
		
		 
		when(userService.findByUserId(userId)).thenReturn(user);
		LocalDateTime localExpiredDateTime = LocalDateTime.now().plusHours(24);
		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setToken(hashedToken);
		refreshToken.setLastUpdatedDate(LocalDateTime.now());
		refreshToken.setUser(user);
		refreshToken.setExpiryDate(localExpiredDateTime);
		when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(refreshToken);
		
		
		
		assertDoesNotThrow(() -> refreshTokenService.saveRefreshToken(userId, token));

	}

	@Test
	void testValidRefreshTokenShouldReturnTrue() throws Exception {
		RefreshToken token = new RefreshToken();
		token.setRevoked(false);
		token.setExpiryDate(LocalDateTime.now().plusMinutes(10));

		boolean result = refreshTokenService.verifyRefreshToken(token);

		assertTrue(result);
	}
	

    @Test
    void verifyRefreshToken_shouldThrowException_whenTokenIsRevoked() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setRevoked(true);
        refreshToken.setExpiryDate(LocalDateTime.now().plusHours(1));
        refreshToken.setToken("revoked-token");

        assertThrows(UserNotFoundException.class, () -> {
            refreshTokenService.verifyRefreshToken(refreshToken);
        });

        verify(refreshTokenRepository, never()).updateRefreshToken(anyBoolean(), any(), any());
    }
    
    @Test
    void verifyRefreshToken_shouldThrowExceptionAndUpdate_whenTokenIsExpiredButNotRevoked() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setRevoked(false);
        refreshToken.setExpiryDate(LocalDateTime.now().minusHours(1));
        refreshToken.setToken("expired-token");

        assertThrows(UserNotFoundException.class, () -> {
            refreshTokenService.verifyRefreshToken(refreshToken);
        });

        verify(refreshTokenRepository).updateRefreshToken(eq(true), any(LocalDateTime.class), eq("expired-token"));
    }
    
    @Test
    void verifyRefreshToken_shouldThrowException_whenTokenIsRevokedAndExpired() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setRevoked(true);
        refreshToken.setExpiryDate(LocalDateTime.now().minusHours(1));
        refreshToken.setToken("expired-revoked-token");

        assertThrows(UserNotFoundException.class, () -> {
            refreshTokenService.verifyRefreshToken(refreshToken);
        });

     
        verify(refreshTokenRepository, never()).updateRefreshToken(anyBoolean(), any(), any());
    }
	
	
	@Test
	void testFindRefreshToken_ReturnsCorrectToken() {
		RefreshToken expectedToken = new RefreshToken();
		expectedToken.setToken(HASHED_TOKEN);
		Mockito.when(GeneralUtility.hashWithSHA256(PLAIN_TOKEN)).thenReturn(HASHED_TOKEN);

		Mockito.when(refreshTokenRepository.findByToken(HASHED_TOKEN)).thenReturn(expectedToken);

		RefreshToken actualToken = refreshTokenService.findRefreshToken(PLAIN_TOKEN);

		assertNotNull(actualToken);
		assertEquals(HASHED_TOKEN, actualToken.getToken());

	}

	
	@Test
	void testGenerateOpaqueRefreshToken() {
		String token = refreshTokenService.generateOpaqueRefreshToken();

		assertNotNull(token);
		assertFalse(token.isEmpty());

		assertTrue(token.matches("^[A-Za-z0-9_-]+$"));

		int minExpectedLength = 43;
		assertTrue(token.length() >= minExpectedLength);
	}

}
