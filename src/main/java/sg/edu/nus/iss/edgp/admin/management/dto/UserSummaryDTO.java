package sg.edu.nus.iss.edgp.admin.management.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSummaryDTO {
	

	private long active;
    private long deleted;
    private long pending;
    private List<UnifiedUserDTO> users;
}
