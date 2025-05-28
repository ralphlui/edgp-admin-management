package sg.edu.nus.iss.edgp.admin.management.service;

import java.util.List;

import sg.edu.nus.iss.edgp.admin.management.dto.RoleDTO;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;

public interface IRoleService {
	
	RoleDTO createRole(Role role);
	
	RoleDTO updateRole(Role role);
	
	List<RoleDTO> findByStatusTrue();
	
	RoleDTO findByRoleName(String roleName);

}
