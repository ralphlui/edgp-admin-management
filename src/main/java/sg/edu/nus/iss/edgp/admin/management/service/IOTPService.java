package sg.edu.nus.iss.edgp.admin.management.service;

public interface IOTPService {
	
	String generateOTP(String email);
	
	String getOtp(String key);
	
	boolean validateOTP(String email, String otp);
	
	String generateRandomOTP();
}
