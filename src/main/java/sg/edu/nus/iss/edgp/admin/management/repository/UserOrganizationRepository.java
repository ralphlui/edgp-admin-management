package sg.edu.nus.iss.edgp.admin.management.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sg.edu.nus.iss.edgp.admin.management.entity.UserOrganization;

@Repository
public interface UserOrganizationRepository extends JpaRepository<UserOrganization, String> {

	List<UserOrganization> findByUser_UserId(String userId);

	Optional<UserOrganization> findByUser_UserIdAndOrgId(String userId, String orgId);


}
