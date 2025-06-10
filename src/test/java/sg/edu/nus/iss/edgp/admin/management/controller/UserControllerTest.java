package sg.edu.nus.iss.edgp.admin.management.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import sg.edu.nus.iss.edgp.admin.management.dto.AuditDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.UserDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.UserInvitationDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.UserRequest;
import sg.edu.nus.iss.edgp.admin.management.dto.ValidationResult;
import sg.edu.nus.iss.edgp.admin.management.entity.RefreshToken;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;
import sg.edu.nus.iss.edgp.admin.management.entity.User;
import sg.edu.nus.iss.edgp.admin.management.entity.UserInvitation;
import sg.edu.nus.iss.edgp.admin.management.exception.UserNotFoundException;
import sg.edu.nus.iss.edgp.admin.management.exception.UserServiceException;
import sg.edu.nus.iss.edgp.admin.management.jwt.JWTService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.AuditService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.RefreshTokenService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.UserInvitationService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.UserService;
import sg.edu.nus.iss.edgp.admin.management.strategy.impl.UserValidationStrategy;
import sg.edu.nus.iss.edgp.admin.management.utility.CookieUtils;
import sg.edu.nus.iss.edgp.admin.management.utility.DTOMapper;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {

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

    private UserRequest userRequest;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        userRequest = new UserRequest();
        userRequest.setEmail("test@example.com");
        userRequest.setUsername("Test User");

        userDTO = new UserDTO();
        userDTO.setEmail("test@example.com");

        when(auditService.createAuditDTO(any(), any(), any(), any(), any())).thenReturn(new AuditDTO());
    }
    
    @AfterEach
    void tearDown() {
        reset(userService, userValidationStrategy,userInvitationService,jwtService, cookieUtils,refreshTokenService,auditService); // etc.
    }

    @Test
    void testCreateUser_success() throws Exception {
        ValidationResult valid = new ValidationResult();
        valid.setValid(true);
        valid.setStatus(HttpStatus.OK);

        when(userValidationStrategy.validateCreation(any(), any())).thenReturn(valid);
        when(userService.createUser(any())).thenReturn(userDTO);

        mockMvc.perform(post("/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    void testCreateUser_validationFail() throws Exception {
        ValidationResult invalid = new ValidationResult();
        invalid.setValid(false);
        invalid.setStatus(HttpStatus.NOT_FOUND);
        invalid.setMessage("Email already exists");

        when(userValidationStrategy.validateCreation(any(), any())).thenReturn(invalid);

        mockMvc.perform(post("/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void testCreateUser_exception() throws Exception {
        ValidationResult valid = new ValidationResult();
        valid.setValid(true);
        valid.setStatus(HttpStatus.OK);

        when(userValidationStrategy.validateCreation(any(), any())).thenReturn(valid);
        when(userService.createUser(any())).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please contact support."));
    }
    
    @Test
    void testUpdateUser_success() throws Exception {
        ValidationResult valid = new ValidationResult();
        valid.setValid(true);
        valid.setStatus(HttpStatus.OK);

        when(userValidationStrategy.validateUpdating(any(), any())).thenReturn(valid);
        when(userService.updateUser(any())).thenReturn(userDTO);

        mockMvc.perform(put("/api/admin/users")
                .header("Authorization", "Bearer token")
                .header("X-User-Id", "123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User updated successfully."));
    }

    @Test
    void testUpdateUser_blankUserId() throws Exception {
        mockMvc.perform(put("/api/admin/users")
                .header("Authorization", "Bearer token")
                .header("X-User-Id", " ")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bad Request:User ID could not be blank."));
    }

    @Test
    void testInviteUser_success() throws Exception {
        ValidationResult valid = new ValidationResult();
        valid.setValid(true);
        valid.setStatus(HttpStatus.OK);

        UserInvitationDTO invitationDTO = new UserInvitationDTO();
        invitationDTO.setEmail("test@example.com");

        when(userValidationStrategy.validateObject(any(), any())).thenReturn(valid);
        when(userInvitationService.createInvitation(any(), any())).thenReturn(invitationDTO);

        mockMvc.perform(post("/api/admin/users/invite")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    void testInviteUser_validationFail() throws Exception {
        ValidationResult invalid = new ValidationResult();
        invalid.setValid(false);
        invalid.setStatus(HttpStatus.NOT_FOUND);
        invalid.setMessage("Invalid user");

        when(userValidationStrategy.validateObject(any(), any())).thenReturn(invalid);

        mockMvc.perform(post("/api/admin/users/invite")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid user"));
    }
    
    @Test
    void testCompleteRegistration_success() throws Exception {
        userRequest.setUserInvitationtoken("valid-token");

        UserInvitation invitation = new UserInvitation();
        invitation.setUsed(false);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(userInvitationService.findByTokenAndEmail(any(), any())).thenReturn(invitation);
        ValidationResult inValid = new ValidationResult();
        inValid.setValid(false);
        when(userValidationStrategy.validateObject(anyString())).thenReturn(inValid);
        when(userService.accountActivate(any(), any())).thenReturn(userDTO);
        when(userInvitationService.updateInvitation(any())).thenReturn(new UserInvitationDTO());

        mockMvc.perform(post("/api/admin/users/complete-registration")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password set successfully. Account activated."));
    }

    @Test
    void testCompleteRegistration_tokenMissing() throws Exception {
        userRequest.setUserInvitationtoken("");

        mockMvc.perform(post("/api/admin/users/complete-registration")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token could not be blank."));
    }

    @Test
    void testCompleteRegistration_invitationInvalid() throws Exception {
        userRequest.setUserInvitationtoken("invalid-token");

        when(userInvitationService.findByTokenAndEmail(any(), any())).thenReturn(null);

        mockMvc.perform(post("/api/admin/users/complete-registration")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invitation token  is invalid."));
    }

    @Test
    void testCompleteRegistration_tokenExpiredOrUsed() throws Exception {
        userRequest.setUserInvitationtoken("expired-token");

        UserInvitation expiredInvitation = new UserInvitation();
        expiredInvitation.setUsed(true);
        expiredInvitation.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(userInvitationService.findByTokenAndEmail(any(), any())).thenReturn(expiredInvitation);

        mockMvc.perform(post("/api/admin/users/complete-registration")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invitation token has expired or already been used."));
    }

    @Test
    void testCompleteRegistration_userAlreadyExists() throws Exception {
        userRequest.setUserInvitationtoken("token-exists");

        UserInvitation invitation = new UserInvitation();
        invitation.setUsed(false);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(userInvitationService.findByTokenAndEmail(any(), any())).thenReturn(invitation);
        ValidationResult valid = new ValidationResult();
        valid.setValid(true);
        when(userValidationStrategy.validateObject(anyString())).thenReturn(valid);

        mockMvc.perform(post("/api/admin/users/complete-registration")
                .header("Authorization", "Bearer token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User already exists."));
    }
    
    @Test
    void testGetAllActiveUsers_success() throws Exception {
        List<UserDTO> users = Collections.singletonList(userDTO);
        Map<Long, List<UserDTO>> resultMap = new LinkedHashMap<>();
        resultMap.put(1L, users);

        when(userService.findActiveUsers(any())).thenReturn(resultMap);

        mockMvc.perform(get("/api/admin/users")
                .param("page", "0")
                .param("size", "10")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].email").value("test@example.com"));
    }

    @Test
    void testGetAllActiveUsers_emptyList() throws Exception {
        Map<Long, List<UserDTO>> resultMap = new LinkedHashMap<>();
        resultMap.put(0L, Collections.emptyList());

        when(userService.findActiveUsers(any())).thenReturn(resultMap);

        mockMvc.perform(get("/api/admin/users")
                .param("page", "0")
                .param("size", "10")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.message").value("No Active User List."));
    }

    @Test
    void testGetAllActiveUsers_exception() throws Exception {
        when(userService.findActiveUsers(any())).thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get("/api/admin/users")
                .param("page", "0")
                .param("size", "10")
                .header("Authorization", "Bearer token"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Retreving active user list is failed due to Unexpected error"));
    }

    @Test
    void testVerifyUser_success() throws Exception {
        userRequest.setAccountVerificationCode("valid-code");

        when(userService.verifyUser(any())).thenReturn(userDTO);

        mockMvc.perform(patch("/api/admin/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User successfully verified."));
    }

    @Test
    void testVerifyUser_blankCode() throws Exception {
        userRequest.setAccountVerificationCode("");

        mockMvc.perform(patch("/api/admin/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Vefriy Id could not be blank."));
    }

    @Test
    void testVerifyUser_exception() throws Exception {
        userRequest.setAccountVerificationCode("valid-code");

        when(userService.verifyUser(any())).thenThrow(new RuntimeException("DB error"));

        mockMvc.perform(patch("/api/admin/users/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User verification is failed due to DB error"));
    }
    
    @Test
    void testLoginUser_success() throws Exception {
        ValidationResult validationResult = new ValidationResult();
        validationResult.setValid(true);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Set-Cookie", "access_token=token");

        when(userValidationStrategy.validateObject(userRequest.getEmail())).thenReturn(validationResult);
        when(userService.loginUser(any(), any())).thenReturn(userDTO);
        when(cookieUtils.buildAuthHeadersWithCookies(any(), any())).thenReturn(headers);

        mockMvc.perform(post("/api/admin/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("test@example.com login successfully"));
    }

    @Test
    void testLoginUser_invalidValidation() throws Exception {
        ValidationResult validationResult = new ValidationResult();
        validationResult.setValid(false);
        validationResult.setMessage("Invalid email");

        when(userValidationStrategy.validateObject(userRequest.getEmail())).thenReturn(validationResult);

        mockMvc.perform(post("/api/admin/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email"));
    }

    @Test
    void testLoginUser_exception() throws Exception {
        ValidationResult validationResult = new ValidationResult();
        validationResult.setValid(true);

        when(userValidationStrategy.validateObject(userRequest.getEmail())).thenReturn(validationResult);
        when(userService.loginUser(any(), any())).thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post("/api/admin/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User failed to login due to Unexpected error"));
    }

    @Test
    void testGetUserProfile_success() throws Exception {
        User user = new User();
        user.setUserId("123");

        when(userService.findActiveUserByID(anyString())).thenReturn(user);
        when(userService.checkSpecificActiveUserByID(anyString())).thenReturn(userDTO);

        mockMvc.perform(get("/api/admin/users/profile")
                .header("Authorization", "Bearer token")
                .header("X-User-Id", "123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.message").value("test@example.com is Active"));
    }

    @Test
    void testGetUserProfile_notFound() throws Exception {
        when(userService.findActiveUserByID(anyString())).thenReturn(null);

        mockMvc.perform(get("/api/admin/users/profile")
                .header("Authorization", "Bearer token")
                .header("X-User-Id", "123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Active User not foud."));
    }

    @Test
    void testGetUserProfile_exception() throws Exception {
        when(userService.findActiveUserByID(anyString())).thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get("/api/admin/users/profile")
                .header("Authorization", "Bearer token")
                .header("X-User-Id", "123"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Retrieving active user by id failed due to Unexpected error"));
    }
    
    @Test
    void testGenerateAccessToken_success() throws Exception {
        when(userService.checkSpecificActiveUserByEmail("test@example.com")).thenReturn(userDTO);
        when(jwtService.generateToken(any())).thenReturn("mocked.jwt.token");

        mockMvc.perform(get("/api/admin/users/accessToken")
                .header("X-User-Email", "test@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("mocked.jwt.token"));
    }

    @Test
    void testGenerateAccessToken_blankEmail() throws Exception {
        mockMvc.perform(get("/api/admin/users/accessToken")
                .header("X-User-Email", ""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid user."));
    }

    @Test
    void testGenerateAccessToken_invalidUser() throws Exception {
        when(userService.checkSpecificActiveUserByEmail("test@example.com")).thenReturn(null);

        mockMvc.perform(get("/api/admin/users/accessToken")
                .header("X-User-Email", "test@example.com"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid user."));
    }

    @Test
    void testGenerateAccessToken_exception() throws Exception {
        when(userService.checkSpecificActiveUserByEmail("test@example.com"))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get("/api/admin/users/accessToken")
                .header("X-User-Email", "test@example.com"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Requesting new access token is failed due to Unexpected error"));
    }
    @Test
    void testRefreshToken_missingToken() throws Exception {
        when(cookieUtils.getTokenFromCookies(any(), eq("refresh_token"))).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/admin/users/refreshToken"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Refresh token is missing"));
    }

    @Test
    void testRefreshToken_invalidToken() throws Exception {
        when(cookieUtils.getTokenFromCookies(any(), eq("refresh_token"))).thenReturn(Optional.of("invalid-token"));
        when(refreshTokenService.findRefreshToken("invalid-token")).thenReturn(null);

        mockMvc.perform(post("/api/admin/users/refreshToken"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired refresh token"));
    }

    @Test
    void testRefreshToken_success() throws Exception {
        RefreshToken refreshToken = new RefreshToken();
        User user = new User();
        user.setUserId("123");
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        Role role = new Role();
        user.setRole(role);
        refreshToken.setUser(user);

        when(cookieUtils.getTokenFromCookies(any(), eq("refresh_token"))).thenReturn(Optional.of("valid-token"));
        when(refreshTokenService.findRefreshToken("valid-token")).thenReturn(refreshToken);
        when(refreshTokenService.verifyRefreshToken(refreshToken)).thenReturn(true);
        when(userService.findByUserIdAndStatus("123", true, true)).thenReturn(user);
        when(cookieUtils.buildAuthHeadersWithCookies(any(), any())).thenReturn(new HttpHeaders());

        mockMvc.perform(post("/api/admin/users/refreshToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token refresh is successful."));
    }
    
    @Test
    void testResetPassword_validRequest() throws Exception {
    	ValidationResult valid = new ValidationResult();
    	valid.setStatus(HttpStatus.OK);
    	valid.setValid(true);
        when(userValidationStrategy.validateObject("test@example.com"))
                .thenReturn(valid);
        when(userService.resetPassword("test@example.com", "password123"))
                .thenReturn(userDTO);

        mockMvc.perform(patch("/api/admin/users/resetPassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Reset Password is completed."));
    }

    @Test
    void testResetPassword_invalidEmail() throws Exception {
    	ValidationResult inValid = new ValidationResult();
    	inValid.setStatus(HttpStatus.BAD_REQUEST);
    	inValid.setValid(false);
    	inValid.setMessage("Invalid email");
        when(userValidationStrategy.validateObject("test@example.com"))
                .thenReturn(inValid);

        mockMvc.perform(patch("/api/admin/users/resetPassword")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email"));
    }

    @Test
    void testLogout_success() throws Exception {
        User user = new User();
        user.setUserId("123");
        user.setEmail("test@example.com");
        user.setUsername("testuser");

        when(cookieUtils.getTokenFromCookies(any(), eq("access_token"))).thenReturn(Optional.of("access-token"));
        when(jwtService.extractUserIdAllowExpiredToken("access-token")).thenReturn("123");
        when(userService.findByUserId("123")).thenReturn(user);
        when(cookieUtils.getTokenFromCookies(any(), eq("refresh_token"))).thenReturn(Optional.of("refresh-token"));
        when(cookieUtils.createCookie(any(), any(), anyBoolean(), anyInt())).thenReturn(mock(ResponseCookie.class));
        when(cookieUtils.createHttpHeader(any(), any())).thenReturn(new HttpHeaders());

        mockMvc.perform(post("/api/admin/users/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User logout successfully"));
    }

    @Test
    void testLogout_userNotFound() throws Exception {
        when(cookieUtils.getTokenFromCookies(any(), eq("access_token"))).thenReturn(Optional.of("access-token"));
        when(jwtService.extractUserIdAllowExpiredToken("access-token")).thenReturn("123");
        when(userService.findByUserId("123")).thenReturn(null);
        when(cookieUtils.createCookie(any(), any(), anyBoolean(), anyInt())).thenReturn(mock(ResponseCookie.class));
        when(cookieUtils.createHttpHeader(any(), any())).thenReturn(new HttpHeaders());

        mockMvc.perform(post("/api/admin/users/logout"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User not found, session cleared."));
    }

    @Test
    void testLogout_exception() throws Exception {
        when(cookieUtils.getTokenFromCookies(any(), eq("access_token"))).thenReturn(Optional.of("access-token"));
        when(jwtService.extractUserIdAllowExpiredToken("access-token")).thenThrow(new RuntimeException("Unexpected error"));
        when(cookieUtils.createCookie(any(), any(), anyBoolean(), anyInt())).thenReturn(mock(ResponseCookie.class));
        when(cookieUtils.createHttpHeader(any(), any())).thenReturn(new HttpHeaders());

        mockMvc.perform(post("/api/admin/users/logout"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please contact support."));
    }
}    

