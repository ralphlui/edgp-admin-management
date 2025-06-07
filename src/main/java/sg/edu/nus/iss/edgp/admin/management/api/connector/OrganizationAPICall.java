package sg.edu.nus.iss.edgp.admin.management.api.connector;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
public class OrganizationAPICall {

	@Value("${organization.api.url}")
	private String organizationURL;

	private static final Logger logger = LoggerFactory.getLogger(OrganizationAPICall.class);
	private static final String GET_SPECIFIC_ACTIVE_USERS_EXCEPTION_MSG = "An unexpected error occurred. Please contact support.{}";

	private static final String UTF_8 = "UTF-8";

	RequestConfig config = RequestConfig.custom().setConnectTimeout(30000).setConnectionRequestTimeout(30000)
			.setSocketTimeout(30000).build();

	public String getOrganization(String orgId, String authorizationHeader) {
		String responseStr = "";

		try (CloseableHttpClient httpClient = HttpClientBuilder.create().setDefaultRequestConfig(config).build()) {
			String url = organizationURL.trim() + "/my-organization";
			logger.info("getOrganization url : " + url);

			HttpGet request = new HttpGet(url);
			request.setHeader("Authorization", authorizationHeader);
			request.setHeader("Content-Type", "application/json");
			request.setHeader("X-Org-Id", orgId);
			CloseableHttpResponse httpResponse = httpClient.execute(request);
			try {
				byte[] responseByteArray = EntityUtils.toByteArray(httpResponse.getEntity());

				responseStr = new String(responseByteArray, Charset.forName(UTF_8));
				logger.info("getOrganization: {}", responseStr);

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
