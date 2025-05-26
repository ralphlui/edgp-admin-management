package sg.edu.nus.iss.edgp.admin.management.configuration.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import sg.edu.nus.iss.edgp.admin.management.dto.RoleDTO;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;
import sg.edu.nus.iss.edgp.admin.management.repository.RoleRepository;
import sg.edu.nus.iss.edgp.admin.management.service.IRoleService;
import sg.edu.nus.iss.edgp.admin.management.utility.DTOMapper;

@Service
public class RoleService implements IRoleService {

	private static final Logger logger = LoggerFactory.getLogger(RoleService.class);

	private RoleRepository roleRepository;

	public RoleService(RoleRepository roleRepository) {
		this.roleRepository = roleRepository;
	}

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
	public Map<Long, List<RoleDTO>> findStatusTrue( Pageable pageable) {
		try {
			
			Page<Role> rolePages =  roleRepository.findStatusTrue(pageable);
			long totalRecord = rolePages.getTotalElements();
			List<RoleDTO> roleDTOList = new ArrayList<>();
			if (totalRecord > 0) {

				for (Role role : rolePages.getContent()) {
					RoleDTO roleDTO = DTOMapper.toRoleDTO(role);
					roleDTOList.add(roleDTO);
				}
			}
			logger.info("Total record in findStatusTrue " + totalRecord);
			Map<Long, List<RoleDTO>> result = new HashMap<>();
			result.put(totalRecord, roleDTOList);
			return result;

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

}
