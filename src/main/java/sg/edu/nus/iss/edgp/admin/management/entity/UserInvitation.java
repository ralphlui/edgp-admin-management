package sg.edu.nus.iss.edgp.admin.management.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserInvitation {
	    
	    @Id
		@UuidGenerator(style = UuidGenerator.Style.AUTO)
		private String inviteId;
		
		@Column(nullable = false)
		private String email;
		
		@Column(nullable = false)
		private String roleName;
		
		@Column(nullable = false)
		private String token;
		
		@Column(nullable = false, columnDefinition = "boolean default false")
		private boolean used;
		
		@Column(nullable = false, columnDefinition = "datetime")
		private LocalDateTime expiresAt;
		
		@Column(nullable = false, columnDefinition = "datetime default now()")
		private LocalDateTime invitedDate;
		
		@Column(nullable = true)
		private String invitedBy;

		@Column(nullable = true, columnDefinition = "datetime")
		private LocalDateTime updatedDate;
		
		@Column(nullable = true)
		private String updatedBy;
	


}
