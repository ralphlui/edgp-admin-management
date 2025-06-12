package sg.edu.nus.iss.edgp.admin.management.jwt;

import io.jsonwebtoken.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import sg.edu.nus.iss.edgp.admin.management.dto.AuditDTO;
import sg.edu.nus.iss.edgp.admin.management.enums.AuditLogInvalidUser;
import sg.edu.nus.iss.edgp.admin.management.enums.HTTPVerb;
import sg.edu.nus.iss.edgp.admin.management.service.impl.AuditService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class JwtFilter extends OncePerRequestFilter {

	private final JWTService jwtService;
	private final AuditService auditLogService;

	 

	@Value("${audit.activity.type.prefix}")
	String activityTypePrefix;

	private String userID;
	private String userName;
	private String apiEndpoint;
	private HTTPVerb httpMethod;
 
	
	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String authHeader = request.getHeader("Authorization");
	    apiEndpoint = request.getRequestURI();
	    String methodName = request.getMethod();
	    httpMethod = HTTPVerb.valueOf(methodName);
	    
	    userID = AuditLogInvalidUser.INVALID_USER_ID.toString();
	    userName = AuditLogInvalidUser.INVALID_USER_NAME.toString();
	    String requestURI = request.getRequestURI(); 

		if (requestURI.contains("accessToekn") || requestURI.contains("/login") || requestURI.contains("/refreshToken") || authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		String jwtToken = authHeader.substring(7); // Remove "Bearer " prefix
	

		if (SecurityContextHolder.getContext().getAuthentication() == null && !jwtToken.isEmpty()) {
			try {
		    userID = jwtService.extractUserIdAllowExpiredToken(jwtToken);	
		    userName = jwtService.extractUserNameAllowExpiredToken(jwtToken);
			UserDetails userDetails = jwtService.getUserDetail(jwtToken);
				if (jwtService.validateToken(jwtToken, userDetails)) {
					UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
							userDetails, null, userDetails.getAuthorities());
					authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

					SecurityContextHolder.getContext().setAuthentication(authentication);
				}
			} catch (ExpiredJwtException e) {
				handleException(response, "JWT token is expired", HttpServletResponse.SC_UNAUTHORIZED,jwtToken);
				return;
			} catch (MalformedJwtException e) {
				handleException(response, "Invalid JWT token", HttpServletResponse.SC_UNAUTHORIZED,jwtToken);
				return;
			} catch (SecurityException e) {
				handleException(response, "JWT signature is invalid", HttpServletResponse.SC_UNAUTHORIZED,jwtToken);
				return;
			} catch (Exception e) {
				handleException(response, e.getMessage(), HttpServletResponse.SC_UNAUTHORIZED,jwtToken);
				return;
			}
			
			
		}

		filterChain.doFilter(request, response);
	}

	private void handleException(HttpServletResponse response, String message, int status, String token)
			throws IOException {
		String activityType = "Authentication-JWTValidation";
		TokenErrorResponse.sendErrorResponse(response, message, status, "UnAuthorized");
		AuditDTO auditDTO = auditLogService.createAuditDTO(userID,activityType, activityTypePrefix, apiEndpoint, httpMethod);
		auditLogService.logAudit(auditDTO, status, message, token);
	}
}