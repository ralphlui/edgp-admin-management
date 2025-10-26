package sg.edu.nus.iss.edgp.admin.management.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserOrganization {

	@Id
	@UuidGenerator(style = UuidGenerator.Style.AUTO)
	private String userOrgId;

	@ManyToOne
	@JoinColumn(name = "userId", nullable = false)
	private User user;

	@Column(nullable = false)
	private String organizationId;

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "roleId")
	private Role role;

	@Column(nullable = false, columnDefinition = "boolean default true")
	private boolean active;

	@Column(nullable = false, columnDefinition = "datetime default now()")
	private LocalDateTime createdDate = LocalDateTime.now();
	
	@Column(nullable = true)
	private String createdBy;
	
	@Column(nullable = true, columnDefinition = "datetime")
	private LocalDateTime updatedDate;
	
	@Column(nullable = true)
	private String updatedBy;

}
