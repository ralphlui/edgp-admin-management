package sg.edu.nus.iss.edgp.admin.management.entity;

import java.util.UUID;

import org.springframework.data.annotation.Id;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID apiKeyOrgMapId;

    @Column(nullable = false, unique = true)
    private String apiKey;

    @Column(nullable = false)
    private String orgId;
}