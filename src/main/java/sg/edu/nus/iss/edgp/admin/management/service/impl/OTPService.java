package sg.edu.nus.iss.edgp.admin.management.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import lombok.AllArgsConstructor;
import sg.edu.nus.iss.edgp.admin.management.utility.GeneralUtility;
import java.security.SecureRandom;
import java.time.Duration;

@AllArgsConstructor
@Service
public class OTPService {

	 
	private static final Logger logger = LoggerFactory.getLogger(OTPService.class);

	private static final int OTP_LENGTH = 6;
	private static final int OTP_VALIDITY_DURATION = 10;
	private static final String DIGITS = "0123456789";
	private static final SecureRandom RANDOM = new SecureRandom();

	
	private final StringRedisTemplate redisTemplate;
	
	
	public String generateOTP(String email) {
		String otp = generateRandomOTP(); // Generate 6-digit OTP
		String hashedEmail = GeneralUtility.hashWithSHA256(email);
		String storedOtp = getOtp(hashedEmail);
		if (storedOtp != null) {
			deleteOtp(hashedEmail);
		}
		redisTemplate.opsForValue().set(hashedEmail, otp, Duration.ofMinutes(OTP_VALIDITY_DURATION));
		logger.info("OTP: {}",  otp);
		return otp;
	}

	private String generateRandomOTP() {
		StringBuilder otp = new StringBuilder();
		for (int i = 0; i < OTP_LENGTH; i++) {
			otp.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
		}
		return otp.toString();
	}

	public String getOtp(String key) {
		return redisTemplate.opsForValue().get(key);
	}

	private void deleteOtp(String key) {
		redisTemplate.opsForValue().getAndDelete(key);
	}

	public boolean validateOTP(String email, String otp) {
		String hashedEmail = GeneralUtility.hashWithSHA256(email);
		String storedOTP = getOtp(hashedEmail);

		if (storedOTP != null && storedOTP.equals(otp)) {
			deleteOtp(hashedEmail);
			return true;
		}
		return false;
	}

}
