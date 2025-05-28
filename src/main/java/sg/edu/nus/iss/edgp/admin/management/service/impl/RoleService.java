package sg.edu.nus.iss.edgp.admin.management.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sg.edu.nus.iss.edgp.admin.management.dto.RoleDTO;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;
import sg.edu.nus.iss.edgp.admin.management.repository.RoleRepository;
import sg.edu.nus.iss.edgp.admin.management.service.IRoleService;
import sg.edu.nus.iss.edgp.admin.management.utility.DTOMapper;
import sg.edu.nus.iss.edgp.admin.management.utility.GeneralUtility;

@Service
public class RoleService implements IRoleService {

	private static final Logger logger = LoggerFactory.getLogger(RoleService.class);

	@Autowired
	private RoleRepository roleRepository;

	@Override
	public RoleDTO createRole(Role role) {
		try {
			role.setCreatedDate(LocalDateTime.now());
			logger.info("Saving Role...");
			Role createdRole = roleRepository.save(role);
			logger.info("Saved successfully...{}", createdRole.getRoleId());
			RoleDTO roleDTO = DTOMapper.toRoleDTO(createdRole);
			return roleDTO;
		} catch (Exception e) {
			logger.error("Error occurred while role creating, " + e.toString());

			throw e;

		}
	}

	@Override
	public List<RoleDTO> findByStatusTrue() {
		try {

			List<Role> roles = roleRepository.findByStatusTrue();

			List<RoleDTO> roleDTOList = new ArrayList<>();

			for (Role role : roles) {
				RoleDTO roleDTO = DTOMapper.toRoleDTO(role);
				roleDTOList.add(roleDTO);
			}

			logger.info("Total record in findStatusTrue " + roles.size());

			return roleDTOList;

		} catch (Exception ex) {
			logger.error("findStatusTrue exception... {}", ex.toString());
			throw ex;
		}

	}

	@Override
	public RoleDTO findByRoleName(String roleName) {
		RoleDTO roleDTO = new RoleDTO();
		try {
			Role role = roleRepository.findByRoleName(roleName);
			roleDTO = DTOMapper.toRoleDTO(role);
		} catch (Exception ex) {
			logger.error("findByRoleName exception... {}", ex.toString());
		}
		return roleDTO;
	}

	@Override
	public RoleDTO updateRole(Role role) {
		RoleDTO roleDTO = new RoleDTO();
		try {
			Optional<Role> dbRole = roleRepository.findById(role.getRoleId());
			dbRole.get().setRoleName(GeneralUtility.makeNotNull(role.getRoleName()));
			dbRole.get().setRoleDescription(GeneralUtility.makeNotNull(role.getRoleDescription()));
			dbRole.get().setStatus(role.isStatus());
			dbRole.get().setUpdatedBy(role.getUpdatedBy());
			dbRole.get().setUpdatedDate(LocalDateTime.now());
			logger.info("Update role...");
			Role savedRole = roleRepository.save(dbRole.get());
			logger.info("Updated successfully...");
			roleDTO = DTOMapper.toRoleDTO(savedRole);

		} catch (Exception ex) {
			logger.error("Role updating exception... {}", ex.toString());

		}

		return roleDTO;
	}

}
