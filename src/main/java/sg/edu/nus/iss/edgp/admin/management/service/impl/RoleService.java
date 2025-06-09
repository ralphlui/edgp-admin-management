package sg.edu.nus.iss.edgp.admin.management.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import sg.edu.nus.iss.edgp.admin.management.dto.RoleDTO;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;
import sg.edu.nus.iss.edgp.admin.management.exception.RoleNotFoundException;
import sg.edu.nus.iss.edgp.admin.management.jwt.JWTService;
import sg.edu.nus.iss.edgp.admin.management.repository.RoleRepository;
import sg.edu.nus.iss.edgp.admin.management.service.IRoleService;
import sg.edu.nus.iss.edgp.admin.management.utility.DTOMapper;
import sg.edu.nus.iss.edgp.admin.management.utility.GeneralUtility;

@RequiredArgsConstructor
@Service
public class RoleService implements IRoleService {

	private static final Logger logger = LoggerFactory.getLogger(RoleService.class);

	private final RoleRepository roleRepository;
	private final JWTService jwtService;

	@Override
	public RoleDTO createRole(Role role) {
		try {
			role.setCreatedDate(LocalDateTime.now());
			
			logger.info("Saving Role...");
			role.setStatus(true);
			Role createdRole = roleRepository.save(role);
			logger.info("Saved successfully...{}", createdRole.getRoleId());
			RoleDTO roleDTO = DTOMapper.toRoleDTO(createdRole);
			return roleDTO;
		} catch (Exception e) {
			logger.error("Error occurred while role creating, " + e.toString());	 
			
		}
		return null;
		
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

		} catch (Exception e) {
			logger.error("findStatusTrue exception... {}", e.toString());
			throw new RoleNotFoundException("An error occured while findByStatusTrue role", e);
		}

	}

	@Override
	public RoleDTO findByRoleName(String roleName) {
		RoleDTO roleDTO = new RoleDTO();
		try {
			Role role = roleRepository.findByRoleName(roleName);
			roleDTO = DTOMapper.toRoleDTO(role);
		} catch (Exception e) {
			logger.error("findByRoleName exception... {}", e.toString());
			throw new RoleNotFoundException("An error occured while findByRoleName role", e);
		}
		return roleDTO;
	}

	@Override
	public RoleDTO updateRole(Role role , String authorizationHeader) {
		
		try {
			RoleDTO roleDTO = new RoleDTO();
			Optional<Role> dbRole = roleRepository.findById(role.getRoleId());
			dbRole.get().setRoleName(GeneralUtility.makeNotNull(role.getRoleName()));
			dbRole.get().setRoleDescription(GeneralUtility.makeNotNull(role.getRoleDescription()));
			dbRole.get().setStatus(role.isStatus());
			dbRole.get().setUpdatedBy(jwtService.getUserIdByAuthHeader(authorizationHeader));
			dbRole.get().setUpdatedDate(LocalDateTime.now());
			logger.info("Update role...");
			Role savedRole = roleRepository.save(dbRole.get());
			logger.info("Updated successfully...");
			roleDTO = DTOMapper.toRoleDTO(savedRole);
			return roleDTO;

		} catch (Exception e) {
			logger.error("Role updating exception... {}", e.toString());

		}

		return null;
	}

}
