package sg.edu.nus.iss.edgp.admin.management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sg.edu.nus.iss.edgp.admin.management.entity.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
	
	Role save (Role role);
	
	List<Role> findByStatusTrue();
	
	Role findByRoleName(String roleName);

}
