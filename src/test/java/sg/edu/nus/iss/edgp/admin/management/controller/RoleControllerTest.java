package sg.edu.nus.iss.edgp.admin.management.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import sg.edu.nus.iss.edgp.admin.management.dto.AuditDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.RoleDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.ValidationResult;
import sg.edu.nus.iss.edgp.admin.management.entity.Role;
import sg.edu.nus.iss.edgp.admin.management.exception.RoleServiceException;
import sg.edu.nus.iss.edgp.admin.management.jwt.JWTService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.AuditService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.RoleService;
import sg.edu.nus.iss.edgp.admin.management.strategy.impl.RoleValidationStrategy;
import sg.edu.nus.iss.edgp.admin.management.utility.DTOMapper;

@WebMvcTest(RoleController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoleService roleService;

    @MockitoBean
    private JWTService jwtService;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private RoleValidationStrategy roleValidationStrategy;

    @Autowired
    private ObjectMapper objectMapper;

    private final String authorizationHeader = "Bearer mock.jwt.token";

    @BeforeEach
    void setUp() {
        when(auditService.createAuditDTO(any(), any(), any(), any(), any()))
                .thenReturn(new AuditDTO());
    }
    
    @AfterEach
    void tearDown() {
        reset(roleService, roleValidationStrategy,jwtService,auditService); // etc.
    }

    @Test
    void testCreateRole_success() throws Exception {
        Role role = new Role("1", "OrgAdmin", "Organization Admin", true, null, null, null, null);
        RoleDTO roleDTO = DTOMapper.toRoleDTO(role);

        ValidationResult validResult = new ValidationResult();
        validResult.setStatus(HttpStatus.OK);
        validResult.setValid(true);

        when(roleValidationStrategy.validateCreation(any(Role.class), eq(""))).thenReturn(validResult);
        when(roleService.createRole(any(Role.class))).thenReturn(roleDTO);

        mockMvc.perform(post("/api/admin/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OrgAdmin is created successfully."))
                .andExpect(jsonPath("$.data.roleName").value("OrgAdmin"));
    }

    @Test
    void testCreateRole_onException() throws Exception {
        Role role = new Role("1", "OrgAdmin", "Organization Admin", true, null, null, null, null);

        ValidationResult validResult = new ValidationResult();
        validResult.setStatus(HttpStatus.OK);
        validResult.setValid(true);

        when(roleValidationStrategy.validateCreation(any(Role.class), eq(""))).thenReturn(validResult);
        when(roleService.createRole(any(Role.class))).thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post("/api/admin/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please contact support."));
    }

    @Test
    void testCreateRole_InternalServerError() throws Exception {
        Role role = new Role("1", "OrgAdmin", "Organization Admin", true, null, null, null, null);

        when(roleValidationStrategy.validateCreation(any(Role.class), any()))
                .thenThrow(new RoleServiceException("Simulated Error"));

        mockMvc.perform(post("/api/admin/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testUpdateRole_success() throws Exception {
        Role role = new Role("123", "OrgAdmin", "Organization Admin", true, null, null, null, null);
        RoleDTO roleDTO = DTOMapper.toRoleDTO(role);

        ValidationResult validResult = new ValidationResult();
        validResult.setStatus(HttpStatus.OK);
        validResult.setValid(true);

        when(roleValidationStrategy.validateUpdating(any(Role.class), eq(authorizationHeader)))
                .thenReturn(validResult);
        when(roleService.updateRole(any(Role.class), eq(authorizationHeader)))
                .thenReturn(roleDTO);

        mockMvc.perform(put("/api/admin/roles")
                        .header("Authorization", authorizationHeader)
                        .header("X-Role-Id", "123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.roleName").value("OrgAdmin"));
    }

    @Test
    void testUpdateRole_blankRoleId() throws Exception {
        Role role = new Role(" ", "OrgAdmin", "Organization Admin", true, null, null, null, null);

        mockMvc.perform(put("/api/admin/roles")
                        .header("Authorization", authorizationHeader)
                        .header("X-Role-Id", " ")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bad Request:Role ID could not be blank."));
    }

    @Test
    void testUpdateRole_blankRoleName() throws Exception {
        Role role = new Role("12345", "", "No name", true, null, null, null, null);

        mockMvc.perform(put("/api/admin/roles")
                        .header("Authorization", authorizationHeader)
                        .header("X-Role-Id", "12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().is5xxServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please contact support."));
    }

    @Test
    void testUpdateRole_NotFound() throws Exception {
        Role role = new Role("123", "NonExistingRole", "Invalid", true, null, null, null, null);

        ValidationResult validationResult = new ValidationResult();
        validationResult.setValid(false);
        validationResult.setStatus(HttpStatus.NOT_FOUND);
        validationResult.setMessage("Role not found");

        when(roleValidationStrategy.validateUpdating(any(Role.class), eq(authorizationHeader)))
                .thenReturn(validationResult);

        mockMvc.perform(put("/api/admin/roles")
                        .header("Authorization", authorizationHeader)
                        .header("X-Role-Id", "123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Role not found"));
    }

    @Test
    void testUpdateRole_exception() throws Exception {
        Role role = new Role("12345", "OrgAdmin", "Organization Admin", true, null, null, null, null);

        ValidationResult validResult = new ValidationResult();
        validResult.setStatus(HttpStatus.OK);
        validResult.setValid(true);

        when(roleValidationStrategy.validateUpdating(any(Role.class), eq(authorizationHeader)))
                .thenReturn(validResult);
        when(roleService.updateRole(any(Role.class), eq(authorizationHeader)))
                .thenThrow(new RoleServiceException("An unexpected error occurred. Please contact support."));

        mockMvc.perform(put("/api/admin/roles")
                        .header("Authorization", authorizationHeader)
                        .header("X-Role-Id", "12345")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(role)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please contact support."));
    }

    @Test
    void testGetAllActiveRoles_InternalServerError() throws Exception {
        when(roleService.findByStatusTrue()).thenThrow(new RoleServiceException("DB error"));

        mockMvc.perform(get("/api/admin/roles")
                        .header("Authorization", authorizationHeader))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("DB error"));
    }

    @Test
    void getAllActiveRoleList_success() throws Exception {
        List<RoleDTO> roleList = new ArrayList<>();
        RoleDTO roleDTO = new RoleDTO();
        roleDTO.setRoleId("1");
        roleDTO.setRoleName("Admin");
        roleList.add(roleDTO);

        when(roleService.findByStatusTrue()).thenReturn(roleList);

        mockMvc.perform(get("/api/admin/roles")
                        .header("Authorization", authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].roleId").value("1"))
                .andExpect(jsonPath("$.totalRecord").value(1));
    }

    @Test
    void getAllActiveRoleList_emptyList() throws Exception {
        when(roleService.findByStatusTrue()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/roles")
                        .header("Authorization", authorizationHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.totalRecord").value(0));
    }

    @Test
    void getAllActiveRoleList_exceptionThrown() throws Exception {
        when(roleService.findByStatusTrue()).thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get("/api/admin/roles")
                        .header("Authorization", authorizationHeader))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please contact support."));
    }
}
