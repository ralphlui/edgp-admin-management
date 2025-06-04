package sg.edu.nus.iss.edgp.admin.management.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PermissionDTO {
 
	private String permissionCode;
	private String moduleName; 
	private String fieldsName; 
	private String sectionName;
	private String remark;

}
