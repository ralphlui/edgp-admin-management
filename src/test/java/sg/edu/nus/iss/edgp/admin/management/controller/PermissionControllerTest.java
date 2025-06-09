package sg.edu.nus.iss.edgp.admin.management.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import sg.edu.nus.iss.edgp.admin.management.dto.AuditDTO;
import sg.edu.nus.iss.edgp.admin.management.dto.PermissionDTO;
import sg.edu.nus.iss.edgp.admin.management.entity.Permission;
import sg.edu.nus.iss.edgp.admin.management.exception.PermissionServiceException;
import sg.edu.nus.iss.edgp.admin.management.jwt.JWTService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.AuditService;
import sg.edu.nus.iss.edgp.admin.management.service.impl.PermissionService;
import sg.edu.nus.iss.edgp.admin.management.utility.DTOMapper;


@WebMvcTest(PermissionController.class)
@AutoConfigureMockMvc(addFilters = false)
class PermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PermissionService permissionService;
    
    @MockitoBean
	private JWTService jwtService;

    @MockitoBean
    private AuditService auditService;
    
    private String authorizationHeader = "Bearer mock.jwt.token";
    
    private Permission p1;

    private Permission p2;
	
	private static PermissionDTO mockDTOP1;
	private static PermissionDTO mockDTOP2;

	private static List<PermissionDTO> mockPermissions;

	@BeforeEach
	void setUp() {
		mockPermissions = new ArrayList<>();
		p1 = new Permission();
		p1.setScope("READ");
		p2 = new Permission();
		p2.setScope("WRITE");
		mockDTOP1 = DTOMapper.toPermissionDTO(p1);
		mockDTOP2 = DTOMapper.toPermissionDTO(p2);
		
		mockPermissions.add(mockDTOP1);
		mockPermissions.add(mockDTOP2);
		
		when(auditService.createAuditDTO(any(), any(), any(), any(), any()))
        .thenReturn(new AuditDTO());
	}

    @Test
    void testGetAllPermissionList_ReturnsPermissionList() throws Exception {
         
        when(permissionService.findPermission()).thenReturn(mockPermissions);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/permissions")
                .header("Authorization", authorizationHeader)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.message").value("Successfully retrieved all permission."))
                .andExpect(jsonPath("$.totalRecord").value(2));
    }

    @Test
    void testGetAllPermissionList_EmptyList() throws Exception {
        when(permissionService.findPermission()).thenReturn(Collections.emptyList());

        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/permissions")
                .header("Authorization", authorizationHeader)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("No Active Permission List."))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void testGetAllPermissionList_internalServerError() throws Exception {
      
        when(permissionService.findPermission())
                .thenThrow(new PermissionServiceException("Database failure"));
 
        mockMvc.perform(MockMvcRequestBuilders.get("/api/admin/permissions")
                        .header("Authorization", authorizationHeader)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.success").value(false));
    }
}
