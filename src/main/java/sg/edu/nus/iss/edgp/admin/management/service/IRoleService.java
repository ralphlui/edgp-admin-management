package sg.edu.nus.iss.edgp.admin.management.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;

import sg.edu.nus.iss.edgp.admin.management.dto.RoleDTO;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;

public interface IRoleService {
	
	RoleDTO createRole(Role role);
	
	Map<Long,List<RoleDTO>> findStatusTrue(Pageable pageable);
	
	RoleDTO findByRoleName(String roleName);

}
