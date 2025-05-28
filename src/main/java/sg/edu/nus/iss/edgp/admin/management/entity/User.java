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
public class User {
	public User() {
		super();
	}

	public User(String email, String username, String password, Role role, UserStatus status) {
		super();
		this.email = email;
		this.username = username;
		this.password = password;
		this.role = role;
		this.status = status;
	}

	@Id
	@UuidGenerator(style = UuidGenerator.Style.AUTO)
	private String userId;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false)
	private String username;

	@Column(nullable = false)
	private String password;

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "roleId")
	private Role role;

	@Column(nullable = false, columnDefinition = "datetime default now()")
	private LocalDateTime createdDate;
	
	@Column(nullable = true)
	private String createdBy;

	@Column(nullable = true, columnDefinition = "datetime")
	private LocalDateTime updatedDate;
	
	@Column(nullable = true)
	private String updatedBy;

	@ManyToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "statusId")
	private UserStatus status;

	@Column(nullable = true, columnDefinition = "datetime")
	private LocalDateTime lastLoginDate;
	
	@Column(nullable = false, columnDefinition = "varchar(255) default ''")
    private String verificationCode;
    
	@Column(nullable = false, columnDefinition = "boolean default false")
    private boolean isVerified;

	
}

