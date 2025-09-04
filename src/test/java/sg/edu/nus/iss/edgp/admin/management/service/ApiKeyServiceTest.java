package sg.edu.nus.iss.edgp.admin.management.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
		when(apiKeyOrgMapRepository.findOrgIdByApiKey(apiKey)).thenReturn(Optional.of(expectedOrgId));

		String result = apiKeyService.retrieveOrgIdByApiKey(apiKey);

		assertEquals(expectedOrgId, result);
		verify(apiKeyOrgMapRepository, times(1)).findOrgIdByApiKey(apiKey);
	}
}
