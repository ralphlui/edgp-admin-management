package sg.edu.nus.iss.edgp.admin.management.strategy.impl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import sg.edu.nus.iss.edgp.admin.management.configuration.JWTConfig;
import sg.edu.nus.iss.edgp.admin.management.dto.RoleDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.ValidationResult;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;
import sg.edu.nus.iss.edgp.admin.management.service.impl.RoleService;
import sg.edu.nus.iss.edgp.admin.management.strategy.IAPIHelperValidationStrategy;
import sg.edu.nus.iss.edgp.admin.management.utility.GeneralUtility;

@AllArgsConstructor
@Service
public class RoleValidationStrategy implements IAPIHelperValidationStrategy <Role>{

	private final RoleService roleService;
	
	@Override
	public ValidationResult validateCreation(Role role, String authorizationHeader) {
		ValidationResult validationResult = new ValidationResult();
		String userId = GeneralUtility.makeNotNull(role.getCreatedBy());

		if (userId.isEmpty()) {
			validationResult.setMessage("Bad Request: User id field could not be blank.");
			validationResult.setValid(false);
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			return validationResult;
		}

		ValidationResult validationObjResult = validateObject(userId, authorizationHeader);
		if (!validationObjResult.isValid()) {
			return validationObjResult;
		}

		if (role.getRoleName() == null || role.getRoleName().isEmpty()) {
			validationResult.setMessage("Bad Request: Role name could not be blank.");
			validationResult.setStatus(HttpStatus.BAD_REQUEST);
			validationResult.setValid(false);
			return validationResult;

		}

		RoleDTO roleDTO = roleService.findByRoleName(role.getRoleName());
		try {
			if (GeneralUtility.makeNotNull(roleDTO.getRoleName().toLowerCase()).equals(role.getRoleName().toLowerCase())) {
				validationResult.setMessage("Store already exists.");
				validationResult.setStatus(HttpStatus.BAD_REQUEST);
				validationResult.setValid(false);
				return validationResult;

			}
		} catch (Exception ex) {
			if (roleDTO.getRoleId() != null) {

				validationResult.setMessage("Role already exists.");
				validationResult.setValid(false);
				return validationResult;

			}
		}
		validationResult.setValid(true);
		return validationResult;
	}

	@Override
	public ValidationResult validateUpdating(Role data, String header) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ValidationResult validateObject(String data) {
		// TODO Auto-generated method stub
		return null;
	}

	
	@Override
	public ValidationResult validateObjectByUserId(Role userId, boolean requiresPasswordValidation) {
		// TODO Auto-generated method stub
		return null;
	}

}
