package sg.edu.nus.iss.edgp.admin.management.entity;

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
public class UserStatus {
	
	@Id
	@UuidGenerator(style = UuidGenerator.Style.AUTO)
	private String statusId;

	
	@Column(nullable = false)
	private String statusdescription;
	

}
