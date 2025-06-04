package sg.edu.nus.iss.edgp.admin.management.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sg.edu.nus.iss.edgp.admin.management.entity.Permission;
import sg.edu.nus.iss.edgp.admin.management.entity.Role; 

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {
 
	
	List<Permission> findAll();
	
	@Query("SELECT p.permissionCode FROM Permission p WHERE p.role.roleId = :roleId")
	List<String> findPermissionCodesByRoleId(@Param("roleId") String roleId);


}
