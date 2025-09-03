package sg.edu.nus.iss.edgp.admin.management.service.impl;

import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.RequiredArgsConstructor;
import sg.edu.nus.iss.edgp.admin.management.exception.UserServiceException;
import sg.edu.nus.iss.edgp.admin.management.repository.ApiKeyOrgMapRepository;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

	private static final Logger log = LoggerFactory.getLogger(ApiKeyService.class);

	private final ApiKeyOrgMapRepository apiKeyOrgMapRepository;

	public String retrieveOrgIdByApiKey(String apiKey) {
		if (apiKey == null || apiKey.isBlank()) {
			log.error("apiKey must not be blank");
			throw new IllegalArgumentException("apiKey must not be blank");
		}

		return apiKeyOrgMapRepository.findOrgIdByApiKey(apiKey).orElseThrow(() -> {
			log.error("Invalid API key: {}", apiKey);
			return new UserServiceException("Invalid API key");
		});
	}
}
