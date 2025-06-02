package sg.edu.nus.iss.edgp.admin.management.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;

import sg.edu.nus.iss.edgp.admin.management.dto.UserDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.UserRequest;
import sg.edu.nus.iss.edgp.admin.management.entity.User;

public interface IUserService {

	 UserDTO createUser (UserRequest user);
	 
	 UserDTO updateUser (UserRequest user);
	 
	 UserDTO verifyUser(String verificationCode);
	 
	 User findByEmail(String email);
	 
	 User findActiveUserByID(String userId);
	 
	 User findByUserId(String userId);
	 
	 Map<Long, List<UserDTO>> findActiveUsers(Pageable pageable);
	 
     UserDTO loginUser(String email, String password);
     
     UserDTO checkSpecificActiveUser(String userId);
	 
	
}
