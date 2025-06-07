package sg.edu.nus.iss.edgp.admin.management.jwt;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.InvalidKeyException;
import lombok.RequiredArgsConstructor;
import sg.edu.nus.iss.edgp.admin.management.configuration.JWTConfig;
import sg.edu.nus.iss.edgp.admin.management.dto.UserDTO;
import sg.edu.nus.iss.edgp.admin.management.entity.User;
import sg.edu.nus.iss.edgp.admin.management.entity.UserOrganization;
import sg.edu.nus.iss.edgp.admin.management.enums.AuditLogInvalidUser;
import sg.edu.nus.iss.edgp.admin.management.repository.UserOrganizationRepository;
import sg.edu.nus.iss.edgp.admin.management.service.impl.PermissionService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.UserService;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@RequiredArgsConstructor
@Service
public class JWTService {

	@Value("${pentest.enable}")
	private String pentestEnable;
	
	@Value("${demo.flag.enable}")
	private String demoEnable;
	
	private final JWTConfig jwtConfig;
	private final ApplicationContext context;
	private final PermissionService permissionService;
	private final UserOrganizationRepository userOrganizationRepository;

	public static final String USER_EMAIL = "userEmail";
	public static final String CLAIM_USERNAME = "userName";

	public PublicKey loadPublicKey() throws Exception {
		byte[] keyBytes = Base64.getDecoder().decode(jwtConfig.getJWTPubliceKey());
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
	}

	public String extractUserID(String token) throws JwtException, IllegalArgumentException, Exception {
		// TODO Auto-generated method stub
		return extractClaim(token, Claims::getSubject);
	}

	public <T> T extractClaim(String token, Function<Claims, T> claimResolver)
			throws JwtException, IllegalArgumentException, Exception {
		final Claims cliams = extractAllClaims(token);
		return claimResolver.apply(cliams);
	}

	public Claims extractAllClaims(String token) throws JwtException, IllegalArgumentException, Exception {
		return Jwts.parser().verifyWith(loadPublicKey()).build().parseSignedClaims(token).getPayload();
	}

	
	public Boolean validateToken(String token, UserDetails userDetails)
			throws JwtException, IllegalArgumentException, Exception {
		Claims claims = extractAllClaims(token);
		String userEmail = claims.get(USER_EMAIL, String.class);
		return (userEmail.equals(userDetails.getUsername()) && !isTokenExpired(token));
	}

	public boolean isTokenExpired(String token) throws JwtException, IllegalArgumentException, Exception {
		return extractExpiration(token).before(new Date());
	}

	public Date extractExpiration(String token) throws JwtException, IllegalArgumentException, Exception {
		return extractClaim(token, Claims::getExpiration);
	}

	public String hashWithSHA256(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashedBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(hashedBytes);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("Error hashing refresh token", e);
		}
	}

	public String retrieveUserID(String authorizationHeader) throws JwtException, IllegalArgumentException, Exception {
		try {
			String token = authorizationHeader.substring(7);
			Claims claims = extractAllClaims(token);
			return claims.getSubject();
		} catch (ExpiredJwtException e) {
			return e.getClaims().getSubject();
		} catch (Exception e) {
			return "Invalid UserID";
		}
	}

	public String retrieveUserName(String token) throws JwtException, IllegalArgumentException, Exception {
		try {
			Claims claims = extractAllClaims(token);
			String userName = claims.get("userName", String.class);
			return userName;
		} catch (ExpiredJwtException e) {
			return e.getClaims().get("userName", String.class);
		} catch (Exception e) {
			return "Invalid Username";
		}
	}

	public String getUserIdByAuthHeader(String authHeader) throws JwtException, IllegalArgumentException, Exception {
		String userID = "";
		String jwtToken = authHeader.substring(7); // Remove "Bearer " prefix
		if (jwtToken != null) {
			userID = extractUserID(jwtToken);

		}
		return userID;
	}

	public String retrieveUserEmail(String authorizationHeader)
			throws JwtException, IllegalArgumentException, Exception {
		try {
			String token = authorizationHeader.substring(7);
			Claims claims = extractAllClaims(token);
			String email = claims.get(USER_EMAIL, String.class);
			return email;
		} catch (ExpiredJwtException e) {
			return e.getClaims().get(USER_EMAIL, String.class);
		} catch (Exception e) {
			return "Invalid userEmail";
		}
	}
	
	public String extractUserIdAllowExpiredToken(String token) throws JwtException, IllegalArgumentException, Exception {
		try {
			return extractClaim(token, Claims::getSubject);
		} catch (ExpiredJwtException e) {
			return e.getClaims().getSubject();
		} catch (Exception e) {
			return AuditLogInvalidUser.INVALID_USER_ID.toString();
		}
	}
	
	public String extractUserNameAllowExpiredToken(String token) throws JwtException, IllegalArgumentException, Exception {
		try {
			Claims claims = extractAllClaims(token);
			String userName = claims.get(CLAIM_USERNAME, String.class);
			return userName;
		} catch (ExpiredJwtException e) {
			return e.getClaims().get(CLAIM_USERNAME, String.class);
		} catch (Exception e) {
			return AuditLogInvalidUser.INVALID_USER_NAME.toString();
		}
	}
	
	private PrivateKey loadPrivateKey() throws Exception {
		byte[] decoded = Base64.getDecoder().decode(jwtConfig.getJWTPrivateKey());
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		return keyFactory.generatePrivate(keySpec);
	}

	
	public String generateToken(UserDTO userDTO)
			throws InvalidKeyException, Exception {
		
		
		long tokenValidDuration;
		
		List<String> scopesFromDb = permissionService.findScopesByRole(userDTO.getRole().getRoleName());
		Set<String> scopes = new HashSet<>(scopesFromDb);
	    
	    // Check if pentest is enabled and adjust token validity to 30 minutes
	    if (pentestEnable.equalsIgnoreCase("true")) {
	        tokenValidDuration = System.currentTimeMillis() + 30 * 60 * 1000;  
	    } // 24 hours for refresh token // 15 minutes for normal token
	    else if (demoEnable.equalsIgnoreCase("true"))  {
	        tokenValidDuration = System.currentTimeMillis() +  5 * 60 * 1000;
	    } else {
	    	tokenValidDuration = System.currentTimeMillis() +  15 * 60 * 1000;
	    }
	    
	    //Get Organization Id by user and role.
	    UserOrganization userOrg= userOrganizationRepository.findByUser_UserIdAndRole_RoleId(userDTO.getUserID(),userDTO.getRole().getRoleId() );
		
		Map<String, Object> claims = new HashMap<>();
		claims.put("userEmail", userDTO.getEmail());
		claims.put(CLAIM_USERNAME, userDTO.getUsername());
		claims.put("scope", String.join(" ", scopes));
		claims.put("orgId",  userOrg.getOrganizationId());
		return Jwts.builder().claims().add(claims).subject(userDTO.getUserID()).issuedAt(new Date(System.currentTimeMillis()))
				.expiration(new Date(tokenValidDuration)).and().signWith(loadPrivateKey(), Jwts.SIG.RS256).compact();
	}
	
	public UserDetails getUserDetail(String token) throws JwtException, IllegalArgumentException, Exception {
		String userID = extractUserID(token);
		User user = context.getBean(UserService.class).findActiveUserByID(userID);
		return org.springframework.security.core.userdetails.User
				.withUsername(user.getEmail()).password(user.getPassword()).roles(user.getRole().toString())
				.build();
	}

}
