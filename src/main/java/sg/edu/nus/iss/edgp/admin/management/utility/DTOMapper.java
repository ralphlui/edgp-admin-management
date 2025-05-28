package sg.edu.nus.iss.edgp.admin.management.utility;

import sg.edu.nus.iss.edgp.admin.management.dto.RoleDTO;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;

public class DTOMapper {
	
	public static RoleDTO toRoleDTO(Role role) {
		RoleDTO roleDTO = new RoleDTO();
		roleDTO.setRoleId(role.getRoleId());
		roleDTO.setRoleName(role.getRoleName());
		roleDTO.setRoleDescription(role.getRoleDescription());
		return roleDTO;
		
	}

}
