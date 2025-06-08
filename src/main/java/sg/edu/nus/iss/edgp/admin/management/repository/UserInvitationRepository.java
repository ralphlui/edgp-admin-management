package sg.edu.nus.iss.edgp.admin.management.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sg.edu.nus.iss.edgp.admin.management.entity.UserInvitation;
import java.util.List;


@Repository
public interface UserInvitationRepository extends JpaRepository<UserInvitation, String>{
	
	Optional<UserInvitation> findByToken(String token);
	
	UserInvitation findByTokenAndEmail(String token, String email);
	
    boolean existsByEmail(String email);
    
    boolean existsByEmailAndUsedIsTrue(String email);
}
