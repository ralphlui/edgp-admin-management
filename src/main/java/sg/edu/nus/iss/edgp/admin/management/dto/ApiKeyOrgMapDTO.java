package sg.edu.nus.iss.edgp.admin.management.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiKeyOrgMapDTO {

	private String apiKey;
	private String orgId;
	private String email;
	private String scope;

}
