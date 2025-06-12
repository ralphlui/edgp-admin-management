package sg.edu.nus.iss.edgp.admin.management.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import sg.edu.nus.iss.edgp.admin.management.dto.*; 
import sg.edu.nus.iss.edgp.admin.management.exception.UserNotFoundException;
import sg.edu.nus.iss.edgp.admin.management.jwt.JWTService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.*;
import sg.edu.nus.iss.edgp.admin.management.strategy.impl.UserValidationStrategy;
import sg.edu.nus.iss.edgp.admin.management.utility.CookieUtils;

@WebMvcTest(OTPController.class)
@AutoConfigureMockMvc(addFilters = false)
public class OTPControllerTest {
	
	@Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserValidationStrategy userValidationStrategy;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserInvitationService userInvitationService;

    @MockitoBean
    private JWTService jwtService;

    @MockitoBean
    private CookieUtils cookieUtils;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private AuditService auditService;
    
    @MockitoBean
    private OTPService otpService;
    

   
	@Test
	void testGenerateOtp_success() throws Exception {
	    String email = "test@example.com";
	    String otp = "123456";
	    String message = "OTP sent to " + email + ". It is valid for 10 minutes.";

	    UserRequest userRequest = new UserRequest();
	    userRequest.setEmail(email);

	    ValidationResult validationResult = new ValidationResult();
	    validationResult.setValid(true);
	    validationResult.setUserId("user-id");
	    validationResult.setUserName("John Doe");

	    UserDTO userDTO = new UserDTO();
	    userDTO.setEmail(email);

	    when(userValidationStrategy.validateObject(email)).thenReturn(validationResult);
	    when(otpService.generateOTP(email)).thenReturn(otp);
	    when(userService.checkSpecificActiveUserByEmail(email)).thenReturn(userDTO);
	    when(auditService.createAuditDTO(any(), any(), any(), any(), any())).thenReturn(new AuditDTO());

	    		mockMvc.perform(post("/api/admin/users/otp/generate")
	            .contentType(MediaType.APPLICATION_JSON)
	            .content(objectMapper.writeValueAsString(userRequest)))
	        .andExpect(status().isOk())
	        .andExpect(jsonPath("$.success").value(true))
	        .andExpect(jsonPath("$.message").value(message))
	        .andExpect(jsonPath("$.data.email").value(email));
	}
	
	@Test
	void testGenerateOtp_invalidEmail() throws Exception {
	    String email = "";
	    UserRequest userRequest = new UserRequest();
	    userRequest.setEmail(email);

	    ValidationResult validationResult = new ValidationResult();
	    validationResult.setValid(false);
	    validationResult.setMessage("Invalid email");

	    when(userValidationStrategy.validateObject(email)).thenReturn(validationResult);
	    when(auditService.createAuditDTO(any(), any(), any(), any(), any())).thenReturn(new AuditDTO());

	    mockMvc.perform(post("/api/admin/users/otp/generate")
	            .contentType(MediaType.APPLICATION_JSON)
	            .content(objectMapper.writeValueAsString(userRequest)))
	        .andExpect(status().isBadRequest())
	        .andExpect(jsonPath("$.success").value(false))
	        .andExpect(jsonPath("$.message").value("Invalid email"));
	}

	@Test
	void testGenerateOtp_unexpectedException() throws Exception {
	    String email = "test@example.com";
	    UserRequest userRequest = new UserRequest();
	    userRequest.setEmail(email);
	    ValidationResult validationResult = new ValidationResult();
	    validationResult.setValid(true);
	    validationResult.setUserId("user-id");
	    validationResult.setUserName("John Doe");

	    when(userValidationStrategy.validateObject(email)).thenReturn(validationResult);
	    when(otpService.generateOTP(email)).thenThrow(new UserNotFoundException("User not found"));
	    when(auditService.createAuditDTO(any(), any(), any(), any(), any())).thenReturn(new AuditDTO());

	    mockMvc.perform(post("/api/admin/users/otp/generate")
	            .contentType(MediaType.APPLICATION_JSON)
	            .content(objectMapper.writeValueAsString(userRequest)))
	        .andExpect(status().is4xxClientError())
	        .andExpect(jsonPath("$.success").value(false));
	}

	@Test
	void testValidateOtp_success() throws Exception {
	    String email = "test@example.com";
	    String otp = "123456";

	    UserRequest userRequest = new UserRequest();
	    userRequest.setEmail(email);
	    userRequest.setOtp(otp);

	    ValidationResult validationResult = new ValidationResult();
	    validationResult.setValid(true);
	    validationResult.setUserId("user-id");
	    validationResult.setUserName("John Doe");

	    UserDTO userDTO = new UserDTO();
	    userDTO.setEmail(email);

	    when(userValidationStrategy.validateObject(email)).thenReturn(validationResult);
	    when(otpService.validateOTP(email, otp)).thenReturn(true);
	    when(userService.checkSpecificActiveUserByEmail(email)).thenReturn(userDTO);
	    when(auditService.createAuditDTO(any(), any(), any(), any(), any())).thenReturn(new AuditDTO());
	    when(cookieUtils.buildAuthHeadersWithCookies(eq(userDTO), any())).thenReturn(new HttpHeaders());

	    mockMvc.perform(post("/api/admin/users/otp/validate")
	            .contentType(MediaType.APPLICATION_JSON)
	            .content(objectMapper.writeValueAsString(userRequest)))
	        .andExpect(status().isOk())
	        .andExpect(jsonPath("$.success").value(true))
	        .andExpect(jsonPath("$.message").value("OTP is valid."))
	        .andExpect(jsonPath("$.data.email").value(email));
	}

	@Test
	void testValidateOtp_emailValidationFailed() throws Exception {
	    UserRequest userRequest = new UserRequest();
	    userRequest.setEmail("invalid-email");

	    ValidationResult validationResult = new ValidationResult();
	    validationResult.setValid(false);
	    validationResult.setMessage("Invalid email");

	    when(userValidationStrategy.validateObject(anyString())).thenReturn(validationResult);
	    when(auditService.createAuditDTO(any(), any(), any(), any(), any())).thenReturn(new AuditDTO());

	    mockMvc.perform(post("/api/admin/users/otp/validate")
	            .contentType(MediaType.APPLICATION_JSON)
	            .content(objectMapper.writeValueAsString(userRequest)))
	        .andExpect(status().is5xxServerError())
	        .andExpect(jsonPath("$.success").value(false))
	        .andExpect(jsonPath("$.message").value("Invalid email"));
	}
	@Test
	void testValidateOtp_otpIncorrect() throws Exception {
	    String email = "test@example.com";
	    String otp = "wrongOtp";

	    UserRequest userRequest = new UserRequest();
	    userRequest.setEmail(email);
	    userRequest.setOtp(otp);

	    ValidationResult validationResult = new ValidationResult();
	    validationResult.setValid(true);
	    validationResult.setUserId("user-id");
	    validationResult.setUserName("John Doe");

	    when(userValidationStrategy.validateObject(email)).thenReturn(validationResult);
	    when(otpService.validateOTP(email, otp)).thenReturn(false);
	    when(auditService.createAuditDTO(any(), any(), any(), any(), any())).thenReturn(new AuditDTO());

	    mockMvc.perform(post("/api/admin/users/otp/validate")
	            .contentType(MediaType.APPLICATION_JSON)
	            .content(objectMapper.writeValueAsString(userRequest)))
	        .andExpect(status().isBadRequest())
	        .andExpect(jsonPath("$.success").value(false));
	}
	@Test
	void testValidateOtp_userNotFoundException() throws Exception {
	    String email = "test@example.com";
	    String otp = "123456";

	    UserRequest userRequest = new UserRequest();
	    userRequest.setEmail(email);
	    userRequest.setOtp(otp);

	    ValidationResult validationResult = new ValidationResult();
	    validationResult.setValid(true);
	    validationResult.setUserId("user-id");
	    validationResult.setUserName("John Doe");

	    when(userValidationStrategy.validateObject(email)).thenReturn(validationResult);
	    when(otpService.validateOTP(email, otp)).thenThrow(new RuntimeException("User not found"));
	    when(auditService.createAuditDTO(any(), any(), any(), any(), any())).thenReturn(new AuditDTO());

	    mockMvc.perform(post("/api/admin/users/otp/validate")
	            .contentType(MediaType.APPLICATION_JSON)
	            .content(objectMapper.writeValueAsString(userRequest)))
	        .andExpect(status().is4xxClientError())
	        .andExpect(jsonPath("$.success").value(false));
	}
	@Test
	void testValidateOtp_internalServerError() throws Exception {
	    String email = "test@example.com";
	    String otp = "123456";

	    UserRequest userRequest = new UserRequest();
	    userRequest.setEmail(email);
	    userRequest.setOtp(otp);

	    ValidationResult validationResult = new ValidationResult();
	    validationResult.setValid(true);
	    validationResult.setUserId("user-id");
	    validationResult.setUserName("John Doe");

	    when(userValidationStrategy.validateObject(email)).thenReturn(validationResult);
	    when(otpService.validateOTP(email, otp)).thenThrow(new UserNotFoundException("Unexpected error"));
	    when(auditService.createAuditDTO(any(), any(), any(), any(), any())).thenReturn(new AuditDTO());

	    mockMvc.perform(post("/api/admin/users/otp/validate")
	            .contentType(MediaType.APPLICATION_JSON)
	            .content(objectMapper.writeValueAsString(userRequest)))
	        .andExpect(status().is4xxClientError())
	        .andExpect(jsonPath("$.success").value(false));
	}

}
