package sg.edu.nus.iss.edgp.admin.management.utility;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import sg.edu.nus.iss.edgp.admin.management.api.connector.NotificationAPICall;
import sg.edu.nus.iss.edgp.admin.management.dto.UserDTO;
import sg.edu.nus.iss.edgp.admin.management.entity.UserInvitation;

@RequiredArgsConstructor
@Component
public class JSONReader {

	private static final Logger logger = LoggerFactory.getLogger(JSONReader.class);
	
	private final NotificationAPICall apiCall;

	public JSONObject parseJsonResponse(String responseStr) throws ParseException {
		if (responseStr == null || responseStr.isEmpty()) {
			return null;
		}

		JSONParser parser = new JSONParser();
		return (JSONObject) parser.parse(responseStr);
	}

	public JSONObject getDataFromResponse(JSONObject jsonResponse) {
		if (jsonResponse != null && !jsonResponse.isEmpty()) {
			return (JSONObject) jsonResponse.get("data");
		}
		return null;
	}

	public String getMessageFromResponse(JSONObject jsonResponse) {
		return (String) jsonResponse.get("message");
	}

	public Boolean getSuccessFromResponse(JSONObject jsonResponse) {
		return (Boolean) jsonResponse.get("success");
	}

	public int getStatusFromResponse(JSONObject jsonResponse) {
		Long status = (Long) jsonResponse.get("status");
		return status.intValue();
	}
	
	public JSONObject sendUserInviteEmail(UserInvitation userInvitation, String authorizationHeader) {

		JSONObject jsonResponse = new JSONObject();

		String responseStr = apiCall.sendUserInviteEmail(userInvitation, authorizationHeader);

		try {

			JSONParser parser = new JSONParser();
			jsonResponse = (JSONObject) parser.parse(responseStr);
			return jsonResponse;

		} catch (ParseException e) {
			
			logger.error("Error parsing JSON response for sendUserInviteEmail... {}", e.toString());

		}

		return jsonResponse;
	}


}
