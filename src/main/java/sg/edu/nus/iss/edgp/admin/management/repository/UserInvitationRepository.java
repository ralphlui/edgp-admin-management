package sg.edu.nus.iss.edgp.admin.management.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sg.edu.nus.iss.edgp.admin.management.entity.Role;
import sg.edu.nus.iss.edgp.admin.management.entity.UserInvitation;

@Repository
public interface UserInvitationRepository extends JpaRepository<UserInvitation, String>{
	
	Optional<UserInvitation> findByToken(String token);
	
	UserInvitation findByTokenAndEmail(String token, String email);
	
    boolean existsByEmail(String email);
    
    boolean existsByEmailAndUsedIsTrue(String email);
    
    List<UserInvitation> findByUsedFalse();
    
    long countByUsed(boolean used);
    
    @Query("SELECT u FROM UserInvitation u WHERE u.used = false AND u.invitedDate >= :invitedDate")
    List<UserInvitation> findValidPendingInvites(@Param("invitedDate") LocalDateTime invitedDate);

}
