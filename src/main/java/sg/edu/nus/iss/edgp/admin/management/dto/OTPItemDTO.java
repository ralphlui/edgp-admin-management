package sg.edu.nus.iss.edgp.admin.management.dto;
 

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OTPItemDTO {
 private String pk;
 private String otp;
 private long expiresAt;
 private long createdAt;
}
