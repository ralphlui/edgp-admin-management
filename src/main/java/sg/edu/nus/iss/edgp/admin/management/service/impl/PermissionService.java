package sg.edu.nus.iss.edgp.admin.management.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sg.edu.nus.iss.edgp.admin.management.dto.PermissionDTO;
import sg.edu.nus.iss.edgp.admin.management.entity.Permission;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;
import sg.edu.nus.iss.edgp.admin.management.repository.PermissionRepository;
import sg.edu.nus.iss.edgp.admin.management.repository.RoleRepository;
import sg.edu.nus.iss.edgp.admin.management.service.IPermissionService;
import sg.edu.nus.iss.edgp.admin.management.utility.DTOMapper;

@Service
public class PermissionService implements IPermissionService {

	private static final Logger logger = LoggerFactory.getLogger(PermissionService.class);

	@Autowired
	private PermissionRepository permissionRepository;

	@Autowired
	private RoleRepository roleRepository;

	@Override
	public List<PermissionDTO> findPermission() {
		try {

			List<Permission> permissions = permissionRepository.findAll();
			List<PermissionDTO> permissionDTOs = new ArrayList<>();

			for (Permission p : permissions) {
				PermissionDTO permissionDTO = DTOMapper.toPermissionDTO(p);
				permissionDTOs.add(permissionDTO);
			}

			logger.info("Total record in findPermission " + permissions.size());

			return permissionDTOs;

		} catch (Exception ex) {
			logger.error("findPermission exception... {}", ex.toString());
			throw ex;
		}
	}

	@Override
	public List<String> findScopesByRole(String roleName) {
		List<String> scopes = new ArrayList<String>();
		try {
			Role role = roleRepository.findByRoleName(roleName);

			if (role != null) {

				scopes = permissionRepository.findScopesByRoleId(role.getRoleId());
			}

			return scopes;
		} catch (Exception ex) {
			logger.error("findPermissionByRole exception... {}", ex.toString());
			throw ex;
		}
	}

}
