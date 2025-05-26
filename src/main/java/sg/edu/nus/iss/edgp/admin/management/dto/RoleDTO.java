package sg.edu.nus.iss.edgp.admin.management.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleDTO {
	
	private String roleId;
	private String roleName;
	private String roleDescription;
	private boolean status;

}
