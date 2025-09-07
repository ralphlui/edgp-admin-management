package sg.edu.nus.iss.edgp.admin.management.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sg.edu.nus.iss.edgp.admin.management.entity.ApiKeyOrgMap;

@Repository
public interface ApiKeyOrgMapRepository extends JpaRepository<ApiKeyOrgMap, String> {

	Optional<ApiKeyOrgMap> findOrgIdByApiKey(@Param("apiKey") String apiKey);
}
