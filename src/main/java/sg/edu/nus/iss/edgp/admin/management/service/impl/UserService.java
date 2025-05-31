package sg.edu.nus.iss.edgp.admin.management.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import sg.edu.nus.iss.edgp.admin.management.dto.UserDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.UserRequest;
import sg.edu.nus.iss.edgp.admin.management.entity.User;
import sg.edu.nus.iss.edgp.admin.management.enums.AuditLogInvalidUser;
import sg.edu.nus.iss.edgp.admin.management.exception.UserNotFoundException;
import sg.edu.nus.iss.edgp.admin.management.jwt.JWTService;
import sg.edu.nus.iss.edgp.admin.management.repository.UserRepository;
import sg.edu.nus.iss.edgp.admin.management.service.IUserService;
import sg.edu.nus.iss.edgp.admin.management.utility.DTOMapper;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService{
 
	private static final Logger logger = LoggerFactory.getLogger(UserService.class);

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JWTService jwtService;
	
	private static final String ACTIVE_USER_NOT_FOUND_MSG = "Active user is not found.";
	private static final String ACTIVE_USER_FOUND_MSG = "Active user is found.";
	private static String INVALID_USER_ID = AuditLogInvalidUser.INVALID_USER_ID.toString();
	private static String INVALID_USER_NAME = AuditLogInvalidUser.INVALID_USER_NAME.toString();
	
	
	@Override
	public UserDTO createUser(UserRequest userReq) {
		try {
			User user = new User();
			user.setEmail(userReq.getEmail());
			user.setUsername(userReq.getUsername());
			String encodedPassword = passwordEncoder.encode(userReq.getPassword());
			user.setPassword(encodedPassword);
			
			user.setVerified(false);
			String code = UUID.randomUUID().toString();
			user.setVerificationCode(code);
			user.setActive(true);
			user.setRole(userReq.getRole());
			
			user.setCreatedDate(LocalDateTime.now());
			 
			logger.info("Create User...");
			User createdUser = userRepository.save(user);
			 
			if (createdUser == null) {
				throw new Exception("User registration is not successful");
			}
			logger.info("User registration is successful.");
			//sendVerificationEmail 
			

			return DTOMapper.toUserDTO(createdUser);

		} catch (Exception e) {
			logger.error("Error occurred while creating user", e);

		}
		return null;
	}

	@Override
	public UserDTO updateUser(UserRequest userReq) {
		try {
			
			User dbUser = findByUserId(userReq.getUserId());
			if (dbUser == null) {
				throw new UserNotFoundException("User not found.");
			}
			dbUser.setUsername(userReq.getUsername());
			dbUser.setPassword(passwordEncoder.encode(userReq.getPassword()));
			dbUser.setActive(userReq.getActive());
			dbUser.setUpdatedDate(LocalDateTime.now());
			logger.info("Update User...");
			User updateUser = userRepository.save(dbUser);
			logger.info("User update is successful");
			return DTOMapper.toUserDTO(updateUser);
		} catch (Exception e) {
			logger.error("Error occurred while user updating", e);
			e.printStackTrace();
			throw e;
		}
	}

	@Override
	public User findByEmail(String email) {
		
		return userRepository.findByEmail(email);
	}

	@Override
	public User findActiveUserByID(String userId) {
		try {
			User user = findByUserIdAndStatus(userId, true, true);
			if (user == null) {
				logger.error(ACTIVE_USER_NOT_FOUND_MSG);
				throw new UserNotFoundException("This user is not an active or verified user");
			}
			logger.info(ACTIVE_USER_FOUND_MSG);
			return user;
			
		} catch (Exception e) {
			logger.error("Error occurred while checking specific active User", e);
			e.printStackTrace();
			throw e;
		}
	}
	
	public User findByUserIdAndStatus(String userId, boolean isActive, boolean isVerified) {

		return userRepository.findByUserIdAndStatus_ActiveAndStatus_Verified(userId, isActive, isVerified);
	}
	

	@Override
	public User findByUserId(String userId) {
		try {
			User user = userRepository.findByUserId(userId);
			return user;
		} catch (Exception e) {
			logger.error("Exception occurred while executing findByUserId", e);
			throw e;
		}
	}
	
	public HashMap<String,String> retrieveUserIDAndNameFromToken(String authorizationHeader)
			throws JwtException, IllegalArgumentException, Exception {
		HashMap<String,String> userInfo = new HashMap<String, String>();
		String jwtToken = authorizationHeader.substring(7);
		INVALID_USER_ID = retrieveUserID(authorizationHeader);
		INVALID_USER_NAME = jwtService.extractUserNameAllowExpiredToken(jwtToken);
		userInfo.put("INVALID_USER_ID", INVALID_USER_ID);
		userInfo.put("INVALID_USER_NAME", INVALID_USER_NAME);
		return userInfo;
	}

	public String retrieveUserID(String authorizationHeader) throws JwtException, IllegalArgumentException, Exception {
		String jwtToken = authorizationHeader.substring(7);
		return jwtService.extractUserIdAllowExpiredToken(jwtToken);
	}

	@Override
	public Map<Long, List<UserDTO>> findActiveUsers(Pageable pageable) {
		Map<Long, List<UserDTO>> result = new HashMap<>();
		List<UserDTO> userDTOList = new ArrayList<>();
		try {
			Page<User> userPages = userRepository.findByStatus_ActiveAndStatus_Verified(true, true, pageable);
			long totalRecord = userPages.getTotalElements();
			if (totalRecord > 0) {
				logger.info("Active user list is found.");
				for (User user : userPages.getContent()) {
					UserDTO userDTO = DTOMapper.toUserDTO(user);
					userDTOList.add(userDTO);
				}
			}
			result.put(totalRecord, userDTOList);
			return result;

		} catch (Exception ex) {
			logger.error("findByIsActiveTrue exception...", ex);
			throw ex;

		}
	}


}
