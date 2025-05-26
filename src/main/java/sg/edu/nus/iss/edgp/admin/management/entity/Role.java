package sg.edu.nus.iss.edgp.admin.management.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
public class Role {
	
	@Id
	@UuidGenerator(style = UuidGenerator.Style.AUTO)
	private String roleId;
	
	@Column(nullable = false)
	private String roleName;
	
	@Column(nullable = false)
	private String roleDescription;
	
	@Column(nullable = false, columnDefinition = "boolean default true")
	private boolean status;
	
	@Column(nullable = false, columnDefinition = "datetime default now()")
	private LocalDateTime createdDate;
	
	@Column(nullable = true)
	private String createdBy;

	@Column(nullable = true, columnDefinition = "datetime")
	private LocalDateTime updatedDate;
	
	@Column(nullable = true)
	private String updatedBy;

}
