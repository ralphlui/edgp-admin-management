package sg.edu.nus.iss.edgp.admin.management.api.connector;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import sg.edu.nus.iss.edgp.admin.management.entity.UserInvitation;

@Service
public class NotificationAPICall {

	@Value("${notification.api.url}")
	private String notificationURL;

	private static final Logger logger = LoggerFactory.getLogger(NotificationAPICall.class);
	private static final String GET_SPECIFIC_ACTIVE_USERS_EXCEPTION_MSG = "An unexpected error occurred. Please contact support.{}";

	private static final String UTF_8 = "UTF-8";

	RequestConfig config = RequestConfig.custom().setConnectTimeout(30000).setConnectionRequestTimeout(30000)
			.setSocketTimeout(30000).build();

	public String sendUserInviteEmail(UserInvitation userInvitation, String authorizationHeader) {
		String responseStr = "";

		try (CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build()) {
			String url = notificationURL.trim() + "/invitation-user";
			logger.info("getSpeicficActiveUsers url : " + url);

			HttpPost request = new HttpPost(url);
			request.setHeader("Authorization", authorizationHeader);
			request.setHeader("Content-Type", "application/json");

			String jsonBody = "{\n"
					+ "  \"userEmail\": \""+userInvitation.getEmail().trim()+"\",\n"
					+ "  \"token\":\""+userInvitation.getToken().trim()+"\"\n"
					+ "}\n"
					+ "";
			request.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));

			CloseableHttpResponse httpResponse = httpClient.execute(request);
			try {
				byte[] responseByteArray = EntityUtils.toByteArray(httpResponse.getEntity());

				responseStr = new String(responseByteArray, Charset.forName(UTF_8));
				logger.info("sendUserInviteEmail: {}", responseStr);

			} catch (Exception e) {
				logger.error(GET_SPECIFIC_ACTIVE_USERS_EXCEPTION_MSG, e.toString());
			} finally {
				try {
					httpResponse.close();
				} catch (IOException e) {
					logger.error(GET_SPECIFIC_ACTIVE_USERS_EXCEPTION_MSG, e.toString());
				}
			}
		} catch (Exception ex) {
			logger.error(GET_SPECIFIC_ACTIVE_USERS_EXCEPTION_MSG, ex.toString());
		}
		return responseStr;
	}

}
