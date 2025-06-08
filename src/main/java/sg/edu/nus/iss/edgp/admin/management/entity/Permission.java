package sg.edu.nus.iss.edgp.admin.management.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
@AllArgsConstructor
public class Permission {
	
	public Permission() {
		super();
	}
	
	@Id
	@UuidGenerator(style = UuidGenerator.Style.AUTO)
	private String permissionId;
	
	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "roleId")
	private Role role;
	
	@Column(nullable = false)
	private String scope;
	
	@Column(nullable = false)
	private String moduleName;
	
	@Column(nullable = false)
	private String fieldsName;
	
	@Column(nullable = false)
	private String sectionName;
	
	@Column(nullable = false)
	private String remark;
	
	@Column(nullable = false, columnDefinition = "datetime default now()")
	private LocalDateTime createdDate;
	
}
