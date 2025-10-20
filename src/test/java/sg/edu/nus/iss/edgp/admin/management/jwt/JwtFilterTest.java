package sg.edu.nus.iss.edgp.admin.management.jwt;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import sg.edu.nus.iss.edgp.admin.management.dto.AuditDTO;
import sg.edu.nus.iss.edgp.admin.management.enums.HTTPVerb;
import sg.edu.nus.iss.edgp.admin.management.service.impl.AuditService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtFilterTest {

    @InjectMocks
    private JwtFilter jwtFilter;

    @Mock
    private JWTService jwtService;

    @Mock
    private AuditService auditService;

    @Mock
    private FilterChain filterChain;

    @Mock
    private UserDetails userDetails;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        jwtFilter = new JwtFilter(jwtService, auditService);
        jwtFilter.activityTypePrefix = "TestPrefix";
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ---------- Helpers ----------
    private void setBearer(String token) {
        request.addHeader("Authorization", "Bearer " + token);
    }

    // ---------- Bypass / allow-through scenarios ----------

    @Test
    void doFilter_skips_whenNoAuthHeader() throws Exception {
        request.setRequestURI("/anything");
        request.setMethod("GET");

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilter_skips_whenLoginOrRefreshEndpoints() throws Exception {
        request.setRequestURI("/login");
        request.setMethod("POST");

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // refresh
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        request.setRequestURI("/refreshToken");
        request.setMethod("POST");

        jwtFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain, times(2)).doFilter(any(), any());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    // ---------- Happy path ----------

    @Test
    void doFilter_setsAuthentication_whenTokenValid() throws Exception {
        String token = "valid.jwt";
        request.setRequestURI("/secure/api");
        request.setMethod("GET");
        setBearer(token);

        when(jwtService.extractUserIdAllowExpiredToken(token)).thenReturn("u1");
        when(jwtService.extractUserNameAllowExpiredToken(token)).thenReturn("name1");
        when(jwtService.getUserDetail(token)).thenReturn(userDetails);
        when(jwtService.validateToken(token, userDetails)).thenReturn(true);
        // No authorities is okay; filter only checks non-null
        when(userDetails.getAuthorities()).thenReturn(null);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(SecurityContextHolder.getContext().getAuthentication()
                instanceof UsernamePasswordAuthenticationToken);
    }

    @Test
    void doFilter_doesNotSetAuthentication_whenValidateTokenFalse() throws Exception {
        String token = "valid.but.not.accepted";
        request.setRequestURI("/secure/api");
        request.setMethod("GET");
        setBearer(token);

        when(jwtService.extractUserIdAllowExpiredToken(token)).thenReturn("u1");
        when(jwtService.extractUserNameAllowExpiredToken(token)).thenReturn("name1");
        when(jwtService.getUserDetail(token)).thenReturn(userDetails);
        when(jwtService.validateToken(token, userDetails)).thenReturn(false);

        jwtFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(200, response.getStatus()); // no error sent
    }

    // ---------- Error paths (401 + audit) ----------

    @Test
    void doFilter_expiredJwt_sends401_andAudits() throws Exception {
        String token = "expired.jwt";
        request.setRequestURI("/secure/api");
        request.setMethod("GET");
        request.addHeader("Authorization", "Bearer " + token);

       
        when(jwtService.extractUserIdAllowExpiredToken(token)).thenReturn("INVALID_USER_ID");
        when(jwtService.extractUserNameAllowExpiredToken(token)).thenReturn("INVALID_USER_NAME");

        
        when(jwtService.getUserDetail(token)).thenThrow(new ExpiredJwtException(null, null, "expired"));

        AuditDTO dto = new AuditDTO();
        when(auditService.createAuditDTO(
                anyString(),
                eq("Authentication-JWTValidation"),
                eq("TestPrefix"),
                eq("/secure/api"),
                eq(HTTPVerb.GET)
        )).thenReturn(dto);

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());

        // Verify auditDTO creation
        verify(auditService, times(1)).createAuditDTO(
                anyString(),
                eq("Authentication-JWTValidation"),
                eq("TestPrefix"),
                eq("/secure/api"),
                eq(HTTPVerb.GET)
        );

       
        verify(auditService, times(1)).logAudit(
                notNull(),
                eq(401),
                contains("expired"),
                eq(token)
        );

       
        verifyNoMoreInteractions(filterChain);
    }
    
    private void stubAuditDTOForCurrentRequest() {
        when(auditService.createAuditDTO(
                anyString(),
                eq("Authentication-JWTValidation"),
                eq(jwtFilter.activityTypePrefix),
                eq(request.getRequestURI()),
                eq(HTTPVerb.valueOf(request.getMethod()))
        )).thenReturn(new AuditDTO());
    }



    @Test
    void doFilter_malformedJwt_sends401_andAudits() throws Exception {
        String token = "malformed.jwt";
        request.setRequestURI("/secure/api");
        request.setMethod("GET");
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtService.extractUserIdAllowExpiredToken(token)).thenReturn("uX");
        when(jwtService.extractUserNameAllowExpiredToken(token)).thenReturn("nX");
        when(jwtService.getUserDetail(token)).thenThrow(new MalformedJwtException("bad"));

        // IMPORTANT: stub Audit DTO creation so logAudit receives non-null
        stubAuditDTOForCurrentRequest();

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());

        verify(auditService, times(1)).createAuditDTO(
                anyString(),
                eq("Authentication-JWTValidation"),
                eq("TestPrefix"),
                eq("/secure/api"),
                eq(HTTPVerb.GET)
        );
        verify(auditService, times(1)).logAudit(
                notNull(),
                eq(401),
                contains("Invalid JWT token"),
                eq(token)
        );
        verifyNoMoreInteractions(filterChain);
    }

    
    @Test
    void doFilter_securityException_sends401_andAudits() throws Exception {
        String token = "bad-sign.jwt";
        request.setRequestURI("/secure/api");
        request.setMethod("GET");
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtService.extractUserIdAllowExpiredToken(token)).thenReturn("uX");
        when(jwtService.extractUserNameAllowExpiredToken(token)).thenReturn("nX");
        when(jwtService.getUserDetail(token)).thenThrow(new SecurityException("sig invalid"));

        stubAuditDTOForCurrentRequest();

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(auditService).logAudit(notNull(), eq(401), contains("signature is invalid"), eq(token));
    }

    @Test
    void doFilter_genericException_sends401_andAudits() throws Exception {
        String token = "boom.jwt";
        request.setRequestURI("/secure/api");
        request.setMethod("GET");
        request.addHeader("Authorization", "Bearer " + token);

        when(jwtService.extractUserIdAllowExpiredToken(token)).thenReturn("uX");
        when(jwtService.extractUserNameAllowExpiredToken(token)).thenReturn("nX");
        when(jwtService.getUserDetail(token)).thenThrow(new RuntimeException("boom"));

        stubAuditDTOForCurrentRequest();

        jwtFilter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        verify(auditService).logAudit(notNull(), eq(401), contains("boom"), eq(token));
    }

}
