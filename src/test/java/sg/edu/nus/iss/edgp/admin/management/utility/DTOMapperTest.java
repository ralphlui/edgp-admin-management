package sg.edu.nus.iss.edgp.admin.management.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import sg.edu.nus.iss.edgp.admin.management.dto.PermissionDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.RoleDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.UserDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.UserInvitationDTO;
import sg.edu.nus.iss.edgp.admin.management.entity.Permission;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;
import sg.edu.nus.iss.edgp.admin.management.entity.User;
import sg.edu.nus.iss.edgp.admin.management.entity.UserInvitation;

@ExtendWith(MockitoExtension.class)
class DTOMapperTest {

	static Role role;

	@BeforeEach
	void setUp() {
		role = new Role();
		role.setRoleId("123");
		role.setRoleName("Admin");
		role.setRoleDescription("Administrator role");
		role.setStatus(true);
	}

	@Test
	void testToRoleDTOSuccessMap() {

		RoleDTO roleDTO = DTOMapper.toRoleDTO(role);

		assertEquals("123", roleDTO.getRoleId());
		assertEquals("Admin", roleDTO.getRoleName());
		assertEquals("Administrator role", roleDTO.getRoleDescription());
		assertTrue(roleDTO.isStatus());
	}

	@Test
	void testToRoleDTO_withNullFields_shouldNotFail() {
		Role role = new Role();

		RoleDTO roleDTO = DTOMapper.toRoleDTO(role);
		assertNull(roleDTO.getRoleId());
		assertNull(roleDTO.getRoleName());
		assertNull(roleDTO.getRoleDescription());
		assertFalse(roleDTO.isStatus()); // default boolean
	}

	@Test
	void testToUserDTOSuccessMap() {

		User user = new User();
		user.setUserId("111");
		user.setUsername("John");
		user.setEmail("john@gmail.com");
		user.setRole(role);
		user.setActive(true);
		user.setVerified(true);
		UserDTO userDTO = DTOMapper.toUserDTO(user);
		assertEquals("111", userDTO.getUserID());
		assertEquals("John", userDTO.getUsername());
		assertEquals("john@gmail.com", userDTO.getEmail());
		assertTrue(userDTO.isActive());

	}

	@Test
	void testToUserDTO_withNullFields_shouldNotFail() {
		User user = new User();
		UserDTO userDTO = DTOMapper.toUserDTO(user);
		assertNull(userDTO.getEmail());

		assertFalse(userDTO.isActive()); // default boolean
	}

	@Test
	void testToPermissionDTOSuccessMap() {
		Permission permission = new Permission();
		permission.setScope("manage:org");
		permission.setFieldsName("Manage");
		permission.setModuleName("OrgMgmt");
		permission.setSectionName("Admin");
		permission.setRemark("Organization");

		PermissionDTO permissionDTO =DTOMapper.toPermissionDTO(permission);

		assertEquals("manage:org", permissionDTO.getScope());
		assertEquals("Manage", permissionDTO.getFieldsName());
		assertEquals("OrgMgmt", permissionDTO.getModuleName());

	}

	@Test
	void testToPermissionDTO_withNullFields_shouldNotFail() {
		Permission permission = new Permission();
		PermissionDTO permissionDTO = DTOMapper.toPermissionDTO(permission);
		assertNull(permissionDTO.getScope());

	}

	@Test
	void testToUserInvitationDTO() {
		UserInvitation userInvitation = new UserInvitation();
		userInvitation.setEmail("emma@gmail.com");
		userInvitation.setRole(role);

		UserInvitationDTO userInvitationDTO = DTOMapper.toUserInvitationDTO(userInvitation);

		assertEquals("emma@gmail.com", userInvitationDTO.getEmail());
		assertEquals("Admin", userInvitationDTO.getRoleName());

	}


}
