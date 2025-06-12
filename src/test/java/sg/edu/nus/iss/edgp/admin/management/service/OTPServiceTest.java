package sg.edu.nus.iss.edgp.admin.management.service;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import sg.edu.nus.iss.edgp.admin.management.configuration.AWSConfig;
import sg.edu.nus.iss.edgp.admin.management.service.impl.OTPService;
import sg.edu.nus.iss.edgp.admin.management.utility.GeneralUtility;


public class OTPServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private AWSConfig awsConfig;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private OTPService otpService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testGenerateOTPStoresInRedis() {
        String email = "test@example.com";
        String hashedEmail = GeneralUtility.hashWithSHA256(email);

        when(redisTemplate.opsForValue().get(hashedEmail)).thenReturn(null);

        String otp = otpService.generateOTP(email);

        assertNotNull(otp);
        assertEquals(6, otp.length());
        verify(valueOperations).set(eq(hashedEmail), eq(otp), eq(Duration.ofMinutes(10)));
    }
    

    @Test
    void testValidateOTP_Success() {
        String email = "test@example.com";
        String otp = "123456";
        String hashedEmail = GeneralUtility.hashWithSHA256(email);

        when(valueOperations.get(hashedEmail)).thenReturn(otp);
        when(redisTemplate.opsForValue().get(hashedEmail)).thenReturn(otp);

        boolean result = otpService.validateOTP(email, otp);

        assertTrue(result);
        verify(valueOperations).getAndDelete(hashedEmail);
    }

    @Test
    void testValidateOTP_Failure() {
        String email = "test@example.com";
        String otp = "123456";
        String hashedEmail = GeneralUtility.hashWithSHA256(email);

        when(valueOperations.get(hashedEmail)).thenReturn("654321");
        when(redisTemplate.opsForValue().get(hashedEmail)).thenReturn("654321");

        boolean result = otpService.validateOTP(email, otp);

        assertFalse(result);
    }

}

