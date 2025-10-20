package sg.edu.nus.iss.edgp.admin.management.service;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import sg.edu.nus.iss.edgp.admin.management.dto.OTPItemDTO;
import sg.edu.nus.iss.edgp.admin.management.dynamo.OTPRepository;
import sg.edu.nus.iss.edgp.admin.management.service.impl.OTPService;
import sg.edu.nus.iss.edgp.admin.management.utility.GeneralUtility;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

class OTPServiceTest {

    private OTPRepository repository;
    private DynamoDbClient dynamo;
    private OTPService service;

    @BeforeEach
    void setUp() {
        repository = mock(OTPRepository.class);
        dynamo = mock(DynamoDbClient.class);
        service = new OTPService(repository, dynamo);
    }


    @Test
    void generateRandomOTP_returnsSixDigits() {
        String otp = service.generateRandomOTP();
        assertThat(otp).hasSize(6).matches("\\d{6}");
    }


    @Test
    void generateOTP_persistsItem_withHashedPk_andReasonableTimestamps() {
        String email = "user@example.com";
        long before = Instant.now().getEpochSecond();

        String otp = service.generateOTP(email);

        
        ArgumentCaptor<OTPItemDTO> cap = ArgumentCaptor.forClass(OTPItemDTO.class);
        verify(repository).put(cap.capture());
        OTPItemDTO saved = cap.getValue();
 
        assertThat(otp).isEqualTo(saved.getOtp()).matches("\\d{6}");
 
        String expectedPk = GeneralUtility.hashWithSHA256(email);
        assertThat(saved.getPk()).isEqualTo(expectedPk);

        long after = Instant.now().getEpochSecond(); 
        assertThat(saved.getCreatedAt()).isBetween(before, after);
 
        long delta = saved.getExpiresAt() - saved.getCreatedAt();
        assertThat(delta).isBetween(590L, 620L);
    }
 

    @Test
    void getOtp_returnsNull_whenItemMissing() {
        when(repository.get("any")).thenReturn(null);
        assertThat(service.getOtp("any")).isNull();
    }

    @Test
    void getOtp_returnsNull_whenExpired() {
        long now = Instant.now().getEpochSecond();
        OTPItemDTO expired = OTPItemDTO.builder()
                .pk("k").otp("111111").createdAt(now - 1000).expiresAt(now - 1).build();
        when(repository.get("k")).thenReturn(expired);

        assertThat(service.getOtp("k")).isNull();
    }

    @Test
    void getOtp_returnsOtp_whenNotExpired() {
        long now = Instant.now().getEpochSecond();
        OTPItemDTO valid = OTPItemDTO.builder()
                .pk("k").otp("222222").createdAt(now - 10).expiresAt(now + 500).build();
        when(repository.get("k")).thenReturn(valid);

        assertThat(service.getOtp("k")).isEqualTo("222222");
    }
 

    @Test
    void validateOTP_returnsFalse_whenItemMissing() {
        when(repository.get(anyString())).thenReturn(null);
        assertThat(service.validateOTP("user@example.com", "000000")).isFalse();
    }

    @Test
    void validateOTP_returnsFalse_whenExpired() {
        String email = "user@example.com";
        String hashed = GeneralUtility.hashWithSHA256(email);
        long now = Instant.now().getEpochSecond();

        OTPItemDTO expired = OTPItemDTO.builder()
                .pk(hashed).otp("123456").createdAt(now - 1000).expiresAt(now - 1).build();

        when(repository.get(eq(hashed))).thenReturn(expired);

        assertThat(service.validateOTP(email, "123456")).isFalse();
        verify(repository, never()).deleteIfMatch(anyString(), anyString());
    }

    @Test
    void validateOTP_returnsFalse_whenOtpMismatch() {
        String email = "user@example.com";
        String hashed = GeneralUtility.hashWithSHA256(email);
        long now = Instant.now().getEpochSecond();

        OTPItemDTO item = OTPItemDTO.builder()
                .pk(hashed).otp("654321").createdAt(now - 5).expiresAt(now + 500).build();

        when(repository.get(eq(hashed))).thenReturn(item);

        assertThat(service.validateOTP(email, "000000")).isFalse();
        verify(repository, never()).deleteIfMatch(anyString(), anyString());
    }

    @Test
    void validateOTP_returnsTrue_whenMatch_andDeleteSucceeds() {
        String email = "user@example.com";
        String hashed = GeneralUtility.hashWithSHA256(email);
        long now = Instant.now().getEpochSecond();

        OTPItemDTO item = OTPItemDTO.builder()
                .pk(hashed).otp("777777").createdAt(now - 5).expiresAt(now + 500).build();

        when(repository.get(eq(hashed))).thenReturn(item);
        when(repository.deleteIfMatch(hashed, "777777")).thenReturn(true);

        assertThat(service.validateOTP(email, "777777")).isTrue();
        verify(repository).deleteIfMatch(hashed, "777777");
    }

    @Test
    void validateOTP_returnsFalse_whenMatch_butDeleteFails() {
        String email = "user@example.com";
        String hashed = GeneralUtility.hashWithSHA256(email);
        long now = Instant.now().getEpochSecond();

        OTPItemDTO item = OTPItemDTO.builder()
                .pk(hashed).otp("888888").createdAt(now - 5).expiresAt(now + 500).build();

        when(repository.get(eq(hashed))).thenReturn(item);
        when(repository.deleteIfMatch(hashed, "888888")).thenReturn(false);

        assertThat(service.validateOTP(email, "888888")).isFalse();
        verify(repository).deleteIfMatch(hashed, "888888");
    }
}

