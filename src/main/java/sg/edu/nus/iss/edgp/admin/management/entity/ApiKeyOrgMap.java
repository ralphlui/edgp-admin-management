package sg.edu.nus.iss.edgp.admin.management.entity;

import org.hibernate.annotations.UuidGenerator;
import jakarta.persistence.Id;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyOrgMap {
	
	@Id
	@UuidGenerator(style = UuidGenerator.Style.AUTO)
	private String apiKeyOrgMapId;

    @Column(nullable = false, unique = true)
    private String apiKey;

    @Column(nullable = false)
    private String orgId;
    
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String scope;
	  
}