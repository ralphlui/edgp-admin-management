package sg.edu.nus.iss.edgp.admin.management.utility;

import sg.edu.nus.iss.edgp.admin.management.dto.PermissionDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.RoleDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.UserDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.UserInvitationDTO;
import sg.edu.nus.iss.edgp.admin.management.entity.Permission;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;
import sg.edu.nus.iss.edgp.admin.management.entity.User;
import sg.edu.nus.iss.edgp.admin.management.entity.UserInvitation;

public class DTOMapper {
	
	public static RoleDTO toRoleDTO(Role role) {
		RoleDTO roleDTO = new RoleDTO();
		roleDTO.setRoleId(role.getRoleId());
		roleDTO.setRoleName(role.getRoleName());
		roleDTO.setRoleDescription(role.getRoleDescription());
		
		roleDTO.setStatus(role.isStatus() ? "Active" : "Deleted");
	    
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
	
	public static PermissionDTO toPermissionDTO(Permission permission) {
		PermissionDTO permissionDTO = new PermissionDTO();
		permissionDTO.setScope(permission.getScope());
		permissionDTO.setFieldsName(permission.getFieldsName());
		permissionDTO.setModuleName(permission.getModuleName());
		permissionDTO.setSectionName(permission.getSectionName());
		permissionDTO.setRemark(permission.getRemark());
		return permissionDTO;
		
	}
	
	public static UserInvitationDTO toUserInvitationDTO(UserInvitation userInvitation) {
		UserInvitationDTO userInvitationDTO = new UserInvitationDTO();
		userInvitationDTO.setEmail(userInvitation.getEmail());
		userInvitationDTO.setRoleName(userInvitation.getRole().getRoleName());
		userInvitationDTO.setStatus(userInvitation.isUsed() ? "Accepted" : "Pending");
		return userInvitationDTO;
				
	}


}
