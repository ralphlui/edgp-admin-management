package sg.edu.nus.iss.edgp.admin.management.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UnifiedUserDTO {
	
	private String id;//enther user id or invitation id
	private String username;
	private String email;
	private String role;
	private String status;
	private String lastLogin;
	private int contributions;


}
