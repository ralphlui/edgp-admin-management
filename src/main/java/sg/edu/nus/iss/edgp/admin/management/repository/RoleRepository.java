package sg.edu.nus.iss.edgp.admin.management.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sg.edu.nus.iss.edgp.admin.management.entity.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
	
	Role save (Role role);
	
	Page<Role> findStatusTrue(Pageable pageable);
	
	Role findByRoleName(String roleName);

}
