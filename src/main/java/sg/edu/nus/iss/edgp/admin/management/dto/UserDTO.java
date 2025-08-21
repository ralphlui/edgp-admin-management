package sg.edu.nus.iss.edgp.admin.management.dto;

import lombok.Getter;
import lombok.Setter;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;

@Getter
@Setter
public class UserDTO {

	private String userID;
	private String email;
	private String username;
	private Role role;
	private boolean isActive;
	private boolean isVerified;
    private String scope;
    
}
