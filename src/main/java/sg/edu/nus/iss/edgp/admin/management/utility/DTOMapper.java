package sg.edu.nus.iss.edgp.admin.management.utility;

import sg.edu.nus.iss.edgp.admin.management.dto.RoleDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.UserDTO;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;
import sg.edu.nus.iss.edgp.admin.management.entity.User;

public class DTOMapper {
	
	public static RoleDTO toRoleDTO(Role role) {
		RoleDTO roleDTO = new RoleDTO();
		roleDTO.setRoleId(role.getRoleId());
		roleDTO.setRoleName(role.getRoleName());
		roleDTO.setRoleDescription(role.getRoleDescription());
		return roleDTO;
		
	}
	public static UserDTO toUserDTO(User user) {
		UserDTO userDTO = new UserDTO();
		userDTO.setUserID(user.getUserId());
		userDTO.setUsername(user.getUsername());
		userDTO.setEmail(user.getEmail());
		userDTO.setRole(user.getRole());
		userDTO.setActive(user.isActive());
		userDTO.setVerified(user.isVerified());
		return userDTO;
	}


}
