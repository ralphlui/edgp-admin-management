package sg.edu.nus.iss.edgp.admin.management.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import sg.edu.nus.iss.edgp.admin.management.dto.ApiKeyOrgMapDTO;
import sg.edu.nus.iss.edgp.admin.management.entity.ApiKeyOrgMap;
import sg.edu.nus.iss.edgp.admin.management.exception.UserServiceException;
import sg.edu.nus.iss.edgp.admin.management.repository.ApiKeyOrgMapRepository;
import sg.edu.nus.iss.edgp.admin.management.service.impl.ApiKeyService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiKeyServiceTest {

	private ApiKeyService apiKeyService;
	private ApiKeyOrgMapRepository apiKeyOrgMapRepository;

	@BeforeEach
	void setUp() {
		apiKeyOrgMapRepository = mock(ApiKeyOrgMapRepository.class);
		apiKeyService = new ApiKeyService(apiKeyOrgMapRepository);
	}

	@Test
	void testRetrieveOrgId_nullApiKey_throwsIllegalArgumentException() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> apiKeyService.retrieveOrgIdByApiKey(null));

		assertEquals("apiKey must not be blank", ex.getMessage());
		verifyNoInteractions(apiKeyOrgMapRepository);
	}

	@Test
	void testRetrieveOrgId_blankApiKey_throwsIllegalArgumentException() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> apiKeyService.retrieveOrgIdByApiKey(" "));

		assertEquals("apiKey must not be blank", ex.getMessage());
		verifyNoInteractions(apiKeyOrgMapRepository);
	}

	@Test
	void testRetrieveOrgId_invalidApiKey_throwsUserServiceException() {
		String apiKey = "invalid-key";
		when(apiKeyOrgMapRepository.findOrgIdByApiKey(apiKey)).thenReturn(Optional.empty());

		UserServiceException ex = assertThrows(UserServiceException.class,
				() -> apiKeyService.retrieveOrgIdByApiKey(apiKey));

		assertEquals("Invalid API key", ex.getMessage());
		verify(apiKeyOrgMapRepository, times(1)).findOrgIdByApiKey(apiKey);
	}

	@Test
	void testRetrieveOrgId_validApiKey_returnsOrgId() {
		String apiKey = "valid-key";
		String expectedOrgId = "ORG-123";
		String email = "test2@gmail.com";
		String scope = "viw:policy";

		ApiKeyOrgMapDTO apiKeyOrgMapDTO = new ApiKeyOrgMapDTO();
		apiKeyOrgMapDTO.setApiKey(apiKey);
		apiKeyOrgMapDTO.setOrgId(expectedOrgId);
		apiKeyOrgMapDTO.setEmail(email);
		apiKeyOrgMapDTO.setScope(scope);
		
		ApiKeyOrgMap apiKeyOrgMap = new ApiKeyOrgMap();
		apiKeyOrgMap.setApiKey(apiKey);
		apiKeyOrgMap.setOrgId(expectedOrgId);
		apiKeyOrgMap.setEmail(email);
		apiKeyOrgMap.setScope(scope);
		
		when(apiKeyOrgMapRepository.findOrgIdByApiKey(apiKey)).thenReturn(Optional.of(apiKeyOrgMap));

		ApiKeyOrgMapDTO dbApiKeyOrgMapDTO  = apiKeyService.retrieveOrgIdByApiKey(apiKey);

		assertEquals(expectedOrgId, dbApiKeyOrgMapDTO.getOrgId());
		verify(apiKeyOrgMapRepository, times(1)).findOrgIdByApiKey(apiKey);
	}
}
