package sg.edu.nus.iss.edgp.admin.management.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;

import sg.edu.nus.iss.edgp.admin.management.dto.PermissionDTO; 

public interface IPermissionService {

	List<PermissionDTO> findPermission();
	
	List<String>findScopesByRole(String roleId);
}
