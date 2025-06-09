package sg.edu.nus.iss.edgp.admin.management.service;


import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value; 
import org.springframework.security.crypto.password.PasswordEncoder; 
import org.springframework.test.context.bean.override.mockito.MockitoBean; 
import sg.edu.nus.iss.edgp.admin.management.configuration.AWSConfig;
import sg.edu.nus.iss.edgp.admin.management.dto.UserRequest;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;
import sg.edu.nus.iss.edgp.admin.management.entity.User;
import sg.edu.nus.iss.edgp.admin.management.repository.UserRepository;
import sg.edu.nus.iss.edgp.admin.management.service.impl.UserService;
import sg.edu.nus.iss.edgp.admin.management.utility.EncryptionUtils;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
	
	private static List<User> mockUsers = new ArrayList<>();

	@InjectMocks
	private UserService userService;

	@MockitoBean
	private UserRepository userRepository;

	@MockitoBean
	private PasswordEncoder passwordEncoder;

	@Mock
	private EncryptionUtils encryptionUtils;
	
	@Mock
    private AWSConfig awsConfig;

      

	@Value("${client.url}")
	private String frontEndUrl = "https://example.com";

	private static User user;

	private static UserRequest userRequest;
	

    private final String encryptedCode = "encryptedCode123";
    private final String decryptedCode = "decryptedCode456";
   
    private static Role role;

	@BeforeEach
	void setUp() {
		
		 role  = new Role("1", "OrgAdmin", "Organization Admin", true, null, null, null, null);
	        
		userRequest = new UserRequest();
		user = new User(userRequest.getEmail(), userRequest.getUsername(), userRequest.getPassword(),
				role, true);
		userRequest.setUserId("8f6e8b84-1219-4c28-a95c-9891c11328b7");
		userRequest.setRole("OrgAdmin");
		user.setUserId(userRequest.getUserId());
		mockUsers.add(user);

	}

	@AfterEach
	void tearDown() {
		user = new User();
		userRequest = new UserRequest();

	}

	
	


}
