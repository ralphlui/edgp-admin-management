package sg.edu.nus.iss.edgp.admin.management.service;

import io.jsonwebtoken.JwtException;
import sg.edu.nus.iss.edgp.admin.management.entity.RefreshToken;

public interface IRefreshTokenService {

	void saveRefreshToken(String userID, String token) throws JwtException, IllegalArgumentException, Exception;
	
	Boolean verifyRefreshToken(RefreshToken refreshToken) throws JwtException, IllegalArgumentException, Exception;
	
	void updateRefreshToken(String token, Boolean revoked);
	
	String generateOpaqueRefreshToken();
	
	RefreshToken findRefreshToken(String refreshToken);
}
