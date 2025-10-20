package sg.edu.nus.iss.edgp.admin.management.utility;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import sg.edu.nus.iss.edgp.admin.management.dto.UserDTO;
import sg.edu.nus.iss.edgp.admin.management.jwt.JWTService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.RefreshTokenService;

@ExtendWith(MockitoExtension.class)
class CookieUtilsTest {

	@Mock
	private JWTService jwtService;

	@Mock
	private RefreshTokenService refreshTokenService;

	@Test
	void testCreateCookie() {
		CookieUtils cookieUtils = new CookieUtils(jwtService, refreshTokenService);

		String name = "token";
		String value = "abc123";
		boolean httpOnly = true;
		long duration = 2L;

		ResponseCookie cookie = cookieUtils.createCookie(name, value, httpOnly, duration);

		assertEquals(cookie.getName(), name);
		assertEquals(cookie.getValue(), value);
		assertEquals(cookie.isHttpOnly(), httpOnly);
	}

	@Test
	void testGetTokenFromCookies_CookieExists() {
		CookieUtils cookieUtils = new CookieUtils(jwtService, refreshTokenService);

		HttpServletRequest request = mock(HttpServletRequest.class);
		Cookie[] cookies = { new Cookie("token", "abc123"), new Cookie("other", "value") };
		when(request.getCookies()).thenReturn(cookies);

		Optional<String> token = cookieUtils.getTokenFromCookies(request, "token");

		assertTrue(token.isPresent());
		assertEquals("abc123", token.get());
	}

	@Test
	void testCreateHttpHeader_AddsBothCookies() {

		CookieUtils cookieUtils = new CookieUtils(jwtService, refreshTokenService);

		ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", "abc123").path("/").httpOnly(true)
				.build();

		ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", "xyz789").path("/").httpOnly(true)
				.build();

		HttpHeaders headers = cookieUtils.createHttpHeader(accessTokenCookie, refreshTokenCookie);

		List<String> setCookieHeaders = headers.get(HttpHeaders.SET_COOKIE);

		assertNotNull(setCookieHeaders);
		assertEquals(2, setCookieHeaders.size());
		assertTrue(setCookieHeaders.contains(accessTokenCookie.toString()));
		assertTrue(setCookieHeaders.contains(refreshTokenCookie.toString()));
	}
	
	@Test
    void testGetTokenFromCookies_NoCookies_returnsEmpty() {
        CookieUtils cookieUtils = new CookieUtils(jwtService, refreshTokenService);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(null);

        Optional<String> token = cookieUtils.getTokenFromCookies(request, "token");
        assertTrue(token.isEmpty());
    }

    @Test
    void testCreateCookie_respectsSecureFlag_true_andSameSiteStrict() {
        CookieUtils cookieUtils = new CookieUtils(jwtService, refreshTokenService);
        // Inject @Value field
        ReflectionTestUtils.setField(cookieUtils, "secureEnable", "true");

        ResponseCookie cookie = cookieUtils.createCookie("token", "v", true, 1L);
        assertTrue(cookie.isSecure()); 
        assertTrue(cookie.isHttpOnly());
        assertEquals("Strict", cookie.getSameSite());
        assertEquals("/", cookie.getPath());
        assertTrue(cookie.getMaxAge().getSeconds() >= 3600);
    }

    @Test
    void testCreateCookie_httpOnlyFalse_forAccessToken() {
        CookieUtils cookieUtils = new CookieUtils(jwtService, refreshTokenService);
        ReflectionTestUtils.setField(cookieUtils, "secureEnable", "false");

        ResponseCookie cookie = cookieUtils.createCookie("access_token", "abc", false, 1L);
        assertFalse(cookie.isHttpOnly());                      // matches method arg
        assertFalse(cookie.isSecure());                        // secure flag set to false
        assertEquals("Strict", cookie.getSameSite());
    }

    @Test
    void testBuildAuthHeadersWithCookies_usesProvidedRefresh_doesNotSave() throws Exception {
        CookieUtils cookieUtils = new CookieUtils(jwtService, refreshTokenService);
        // Secure cookie on
        ReflectionTestUtils.setField(cookieUtils, "secureEnable", "true");

        UserDTO user = new UserDTO();
        user.setUserID("U1");
        user.setEmail("x@x.com");
        user.setUsername("x");

        when(jwtService.generateToken(user)).thenReturn("ATOKEN");

        // Call with a pre-existing refresh token
        HttpHeaders headers = cookieUtils.buildAuthHeadersWithCookies(user, "R-TOKEN");

         verify(refreshTokenService, never()).saveRefreshToken(anyString(), anyString());

        List<String> setCookie = headers.get(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        assertEquals(2, setCookie.size());

        // String form of ResponseCookie is what gets added to headers
        String c1 = setCookie.get(0);
        String c2 = setCookie.get(1);
        String all = c1 + " || " + c2;

        assertTrue(all.contains("access_token=ATOKEN"));
        assertTrue(all.contains("refresh_token=R-TOKEN"));

        // access token cookie: HttpOnly should be absent (false)
        assertFalse(all.contains("access_token=ATOKEN; HttpOnly"));

        // refresh token cookie: HttpOnly must be present (true)
        assertTrue(all.contains("refresh_token=R-TOKEN") && all.contains("HttpOnly"));

        // Secure should be present because secureEnable="true"
        assertTrue(all.contains("Secure"));

        // SameSite=Strict
        assertTrue(all.contains("SameSite=Strict"));
        // Path=/
        assertTrue(all.contains("Path=/"));
        // Max-Age for 1 hour is 3600 (exact formatting can vary by Spring version, so do a contains)
        assertTrue(all.contains("Max-Age=3600"));
    }

    @Test
    void testBuildAuthHeadersWithCookies_generatesRefreshAndSaves_whenNullRefresh() throws Exception {
        CookieUtils cookieUtils = new CookieUtils(jwtService, refreshTokenService);
        // Secure cookie off
        ReflectionTestUtils.setField(cookieUtils, "secureEnable", "false");

        UserDTO user = new UserDTO();
        user.setUserID("U2");
        user.setEmail("y@y.com");
        user.setUsername("y");

        when(jwtService.generateToken(user)).thenReturn("AT2");
        when(refreshTokenService.generateOpaqueRefreshToken()).thenReturn("GENREF");

        HttpHeaders headers = cookieUtils.buildAuthHeadersWithCookies(user, null);
 
        verify(refreshTokenService, times(1)).saveRefreshToken("U2", "GENREF");

        List<String> setCookie = headers.get(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        assertEquals(2, setCookie.size());

        String all = String.join(" || ", setCookie);
        assertTrue(all.contains("access_token=AT2"));
        assertTrue(all.contains("refresh_token=GENREF"));

        // Secure=false now
        assertFalse(all.contains("Secure"));

        // HttpOnly only on refresh cookie
        assertFalse(all.contains("access_token=AT2; HttpOnly"));
        assertTrue(all.contains("refresh_token=GENREF") && all.contains("HttpOnly"));
    }

}
