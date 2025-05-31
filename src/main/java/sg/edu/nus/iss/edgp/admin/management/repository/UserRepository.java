package sg.edu.nus.iss.edgp.admin.management.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
import sg.edu.nus.iss.edgp.admin.management.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, String>  {
	
	User save(User user);
	
    User findByEmail(String email);
	
	User findByUserId(String userId);
	
	Page<User> findByStatus_ActiveTrue(Pageable pageable);

	User findByUserIdAndStatus_ActiveAndStatus_Verified(String userId, boolean active, boolean verified);
	
	Page<User> findByStatus_ActiveAndStatus_Verified(boolean active, boolean verified, Pageable pageable);

}
