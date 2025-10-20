package sg.edu.nus.iss.edgp.admin.management.api.connector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.util.EntityUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import sg.edu.nus.iss.edgp.admin.management.entity.UserInvitation;

@ExtendWith(MockitoExtension.class)
class NotificationAPICallTest {

    private MockedStatic<HttpClientBuilder> httpClientBuilderStatic;

    private HttpClientBuilder mockBuilder() {
        return mock(HttpClientBuilder.class, RETURNS_DEEP_STUBS);
    }

    private NotificationAPICall setupServiceWithUrl(String baseUrl) {
        NotificationAPICall svc = new NotificationAPICall();
       
        ReflectionTestUtils.setField(svc, "notificationURL", baseUrl);
        return svc;
    }

    @AfterEach
    void tearDown() {
        if (httpClientBuilderStatic != null) {
            httpClientBuilderStatic.close();
            httpClientBuilderStatic = null;
        }
    }

    @Test
    void sendUserInviteEmail_success_sendsJsonWithTrimmedValues_andReturnsBody() throws Exception {
       
        String baseUrl = "https://notify.example.com";
        NotificationAPICall svc = setupServiceWithUrl(baseUrl);

        
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);

        httpClientBuilderStatic = mockStatic(HttpClientBuilder.class);
        HttpClientBuilder builder = mockBuilder();
        httpClientBuilderStatic.when(HttpClientBuilder::create).thenReturn(builder);
        when(builder.setDefaultRequestConfig(any(RequestConfig.class))).thenReturn(builder);
        when(builder.build()).thenReturn(httpClient);

       
        String body = "{\"sent\":true}";
        HttpEntity entity = new ByteArrayEntity(body.getBytes(StandardCharsets.UTF_8));
        when(response.getEntity()).thenReturn(entity);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);

   
        UserInvitation invite = mock(UserInvitation.class);
        when(invite.getEmail()).thenReturn(" user@example.com ");  // with spaces
        when(invite.getToken()).thenReturn("  abc123  ");          // with spaces

     
        String result = svc.sendUserInviteEmail(invite, "Bearer xyz");

        
        assertThat(result).isEqualTo(body);

        
        ArgumentCaptor<HttpPost> captor = ArgumentCaptor.forClass(HttpPost.class);
        verify(httpClient).execute(captor.capture());
        HttpPost actual = captor.getValue();

       
        assertThat(actual.getURI().toString())
                .isEqualTo("https://notify.example.com/invitation-user");

      
        assertThat(actual.getFirstHeader("Authorization").getValue()).isEqualTo("Bearer xyz");
        assertThat(actual.getFirstHeader("Content-Type").getValue()).isEqualTo("application/json");

      
        String sentJson = EntityUtils.toString(actual.getEntity(), StandardCharsets.UTF_8);
        assertThat(sentJson).contains("\"userEmail\": \"user@example.com\"");
        assertThat(sentJson).contains("\"token\":\"abc123\"");
        assertThat(sentJson).contains("\"userEmail\"");
        assertThat(sentJson).contains("\"token\"");

       
        verify(response, times(1)).getEntity();
        verify(response, times(1)).close();
        verify(httpClient, times(1)).close();
    }

    @Test
    void sendUserInviteEmail_executeThrows_returnsEmptyString() throws Exception {
       
        NotificationAPICall svc = setupServiceWithUrl("https://notify.example.com");

        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);

        httpClientBuilderStatic = mockStatic(HttpClientBuilder.class);
        HttpClientBuilder builder = mockBuilder();
        httpClientBuilderStatic.when(HttpClientBuilder::create).thenReturn(builder);
        when(builder.setDefaultRequestConfig(any(RequestConfig.class))).thenReturn(builder);
        when(builder.build()).thenReturn(httpClient);

        when(httpClient.execute(any(HttpPost.class))).thenThrow(new IOException("boom"));

       
        UserInvitation invite = mock(UserInvitation.class);
        when(invite.getEmail()).thenReturn("user@example.com");
        when(invite.getToken()).thenReturn("abc123");

       
        String result = svc.sendUserInviteEmail(invite, "Bearer xyz");

       
        assertThat(result).isEmpty();
        verify(httpClient, times(1)).close();
    }

    @Test
    void sendUserInviteEmail_nullEntity_returnsEmptyString() throws Exception {
       
        NotificationAPICall svc = setupServiceWithUrl("https://notify.example.com");

        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);

        httpClientBuilderStatic = mockStatic(HttpClientBuilder.class);
        HttpClientBuilder builder = mockBuilder();
        httpClientBuilderStatic.when(HttpClientBuilder::create).thenReturn(builder);
        when(builder.setDefaultRequestConfig(any(RequestConfig.class))).thenReturn(builder);
        when(builder.build()).thenReturn(httpClient);

        when(response.getEntity()).thenReturn(null);
        when(httpClient.execute(any(HttpPost.class))).thenReturn(response);

        UserInvitation invite = mock(UserInvitation.class);
        when(invite.getEmail()).thenReturn("user@example.com");
        when(invite.getToken()).thenReturn("abc123");

   
        String result = svc.sendUserInviteEmail(invite, "Bearer xyz");

        
        assertThat(result).isEmpty();
        verify(response, times(1)).getEntity();
        verify(response, times(1)).close();
        verify(httpClient, times(1)).close();
    }
}
