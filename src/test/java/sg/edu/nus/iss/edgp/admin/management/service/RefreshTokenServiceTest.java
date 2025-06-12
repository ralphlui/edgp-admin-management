package sg.edu.nus.iss.edgp.admin.management.service;


import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import sg.edu.nus.iss.edgp.admin.management.entity.RefreshToken;
import sg.edu.nus.iss.edgp.admin.management.entity.User;
import sg.edu.nus.iss.edgp.admin.management.exception.UserNotFoundException;
import sg.edu.nus.iss.edgp.admin.management.repository.RefreshTokenRepository;
import sg.edu.nus.iss.edgp.admin.management.service.impl.RefreshTokenService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.UserService;
import sg.edu.nus.iss.edgp.admin.management.utility.GeneralUtility;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private final String userId = "user123";
    private final String plainToken = "test-token";
    private final String hashedToken = GeneralUtility.hashWithSHA256(plainToken);

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setUserId(userId);
    }
    
    @Test
    void testSaveRefreshToken_success() throws Exception {
        when(userService.findByUserId(userId)).thenReturn(mockUser);

        refreshTokenService.saveRefreshToken(userId, plainToken);

        verify(userService).findByUserId(userId);
        
    }
    
    @Test
    void testUpdateRefreshToken_success() {
        refreshTokenService.updateRefreshToken(plainToken, true);

        verify(refreshTokenRepository).updateRefreshToken(eq(true), any(LocalDateTime.class), eq(hashedToken));
    }
    
    @Test
    void testUpdateRefreshToken_failure() {
        doThrow(new RuntimeException("DB error"))
                .when(refreshTokenRepository)
                .updateRefreshToken(anyBoolean(), any(), anyString());

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
                refreshTokenService.updateRefreshToken(plainToken, true));

        assertEquals("Error updating refresh token", thrown.getMessage());
    }


    @Test
    void testVerifyRefreshToken_revoked() {
        RefreshToken token = new RefreshToken();
        token.setRevoked(true);
        token.setExpiryDate(LocalDateTime.now().plusHours(1));

        assertThrows(UserNotFoundException.class, () ->
                refreshTokenService.verifyRefreshToken(token));
    }

    @Test
    void testVerifyRefreshToken_expired() {
        RefreshToken token = new RefreshToken();
        token.setRevoked(false);
        token.setExpiryDate(LocalDateTime.now().minusMinutes(1));
        token.setToken(hashedToken);

        assertThrows(UserNotFoundException.class, () ->
                refreshTokenService.verifyRefreshToken(token));

        verify(refreshTokenRepository).updateRefreshToken(eq(true), any(), eq(hashedToken));
    }
    
    @Test
    void testFindRefreshToken_success() {
        RefreshToken token = new RefreshToken();
        when(refreshTokenRepository.findByToken(hashedToken)).thenReturn(token);

        RefreshToken result = refreshTokenService.findRefreshToken(plainToken);
        assertEquals(token, result);
    }
    
    @Test
    void testGenerateOpaqueRefreshToken() {
        String token = refreshTokenService.generateOpaqueRefreshToken();
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }
}
