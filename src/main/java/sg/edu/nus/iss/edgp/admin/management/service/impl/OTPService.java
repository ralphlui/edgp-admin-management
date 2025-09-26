package sg.edu.nus.iss.edgp.admin.management.service.impl;


import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import sg.edu.nus.iss.edgp.admin.management.dto.OTPItemDTO;
import sg.edu.nus.iss.edgp.admin.management.repository.OTPRepository;
import sg.edu.nus.iss.edgp.admin.management.service.IOTPService;
import sg.edu.nus.iss.edgp.admin.management.utility.GeneralUtility;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;


@Service
@RequiredArgsConstructor
public class OTPService implements IOTPService {

 private static final Logger logger = LoggerFactory.getLogger(OTPService.class);

 private static final int OTP_LENGTH = 6;
 private static final int OTP_VALIDITY_DURATION_MIN = 10; // minutes
 private static final String DIGITS = "0123456789";
 private static final SecureRandom RANDOM = new SecureRandom();

 private final OTPRepository repository;


 @Override
 public String generateOTP(String email) {
     String otp = generateRandomOTP();
     String hashedEmail = GeneralUtility.hashWithSHA256(email);

     long now = Instant.now().getEpochSecond();
     long expiresAt = Instant.now().plus(OTP_VALIDITY_DURATION_MIN, ChronoUnit.MINUTES).getEpochSecond();

     OTPItemDTO item = OTPItemDTO.builder()
             .pk(hashedEmail)
             .otp(otp)
             .createdAt(now)
             .expiresAt(expiresAt)
             .build();

     repository.put(item);
     logger.info("Generated OTP for email hash {} (expires in {} min)", hashedEmail, OTP_VALIDITY_DURATION_MIN);
     return otp;
 }
 
 @Override
 public String getOtp(String key) {
	 OTPItemDTO item = repository.get(key);
     if (item == null) return null;

     long now = Instant.now().getEpochSecond();
     if (item.getExpiresAt() <= now) {
         // already expired — let TTL clean it up, but treat as missing
         return null;
     }
     return item.getOtp();
 }

 @Override
 public boolean validateOTP(String email, String otp) {
     String hashedEmail = GeneralUtility.hashWithSHA256(email);
     OTPItemDTO item = repository.get(hashedEmail);

     if (item == null) return false;

     long now = Instant.now().getEpochSecond();
     if (item.getExpiresAt() <= now) {
         // expired
         return false;
     }

     if (!otp.equals(item.getOtp())) {
         return false;
     }

     // Single-use: delete only if OTP matches
     boolean deleted = repository.deleteIfMatch(hashedEmail, otp);
     return deleted;
 }

 @Override
 public String generateRandomOTP() {
     StringBuilder sb = new StringBuilder(OTP_LENGTH);
     for (int i = 0; i < OTP_LENGTH; i++) {
         sb.append(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
     }
     return sb.toString();
 }
}
