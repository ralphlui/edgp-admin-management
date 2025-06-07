package sg.edu.nus.iss.edgp.admin.management.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserRequest {

	private String email;
	private String userId;
	private String username;
	private String password;
	private Boolean active;
	private String role;
	private String organizationId;
	private String accountVerificationCode; 
    private String otp;
    private String userInvitationtoken;
	
}
