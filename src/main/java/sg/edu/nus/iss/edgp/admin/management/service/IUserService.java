package sg.edu.nus.iss.edgp.admin.management.service;

import sg.edu.nus.iss.edgp.admin.management.dto.UserDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.UserRequest;
import sg.edu.nus.iss.edgp.admin.management.entity.User;

public interface IUserService {

	 UserDTO createUser (UserRequest user);
	 
	 UserDTO updateUser (UserRequest user);
	 
	 User findByEmail(String email);
	 
	 User findActiveUserByID(String userId);
	 
	 User findByUserId(String userId);
}
