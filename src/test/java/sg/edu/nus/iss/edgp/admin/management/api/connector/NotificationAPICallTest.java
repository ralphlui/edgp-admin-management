package sg.edu.nus.iss.edgp.admin.management.api.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import sg.edu.nus.iss.edgp.admin.management.entity.UserInvitation;


class NotificationAPICallTest {
	
	private NotificationAPICall notiAPICall;

	@Mock
	private HttpClient httpClient;

	@Mock
	private HttpResponse<String> httpResponse;

	@Mock
	private HttpClient httpClientMock;
	
	@Mock
	private HttpResponse<String> httpResponseMock;
	
	UserInvitation userInvitation;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		
		userInvitation = new UserInvitation();
		userInvitation.setEmail("john@gmail.com");
		userInvitation.setToken("token");
		userInvitation.setOrganizationId("org1");
		notiAPICall = new NotificationAPICall();

		try {
			java.lang.reflect.Field field = NotificationAPICall.class.getDeclaredField("notificationURL");
			field.setAccessible(true);
			field.set(notiAPICall, "http://test-noti-url.com/");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void testValidateActiveUser_ExceptionHandling() throws Exception {
		when(httpClient.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
				.thenThrow(new RuntimeException("Connection error"));

		String result = notiAPICall.sendUserInviteEmail(userInvitation, "Bearer xyz");

		assertEquals("", result);
	}

	@Test
	void testHttpClientSendReturnsExpectedResponse() throws Exception {
		String expectedResponseBody = "{\"success\":\"true\"}";

		when(httpResponseMock.body()).thenReturn(expectedResponseBody);

		when(httpClientMock.send(any(HttpRequest.class), Mockito.<HttpResponse.BodyHandler<String>>any()))
				.thenReturn(httpResponseMock);

		HttpRequest request = HttpRequest.newBuilder().uri(new java.net.URI("http://example.com")).GET().build();

		HttpResponse<String> response = httpClientMock.send(request, HttpResponse.BodyHandlers.ofString());
		String actualBody = response.body();

		assertEquals(expectedResponseBody, actualBody);
	}

}
