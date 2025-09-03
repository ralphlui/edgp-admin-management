package sg.edu.nus.iss.edgp.admin.management.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sg.edu.nus.iss.edgp.admin.management.entity.ApiKeyOrgMap;

@Repository
public interface ApiKeyOrgMapRepository extends JpaRepository<ApiKeyOrgMap, UUID> {

	 Optional<String> findOrgIdByApiKey(String apiKey);
}
