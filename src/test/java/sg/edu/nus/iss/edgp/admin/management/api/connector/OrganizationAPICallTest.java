package sg.edu.nus.iss.edgp.admin.management.api.connector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrganizationAPICallTest {

    private MockedStatic<HttpClientBuilder> httpClientBuilderStatic;

    private HttpClientBuilder mockBuilder() {
        return mock(HttpClientBuilder.class, RETURNS_DEEP_STUBS);
    }

    private OrganizationAPICall setupServiceWithUrl(String baseUrl) {
        OrganizationAPICall svc = new OrganizationAPICall();
        
        ReflectionTestUtils.setField(svc, "organizationURL", baseUrl);
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
    void getOrganization_success_returnsResponseBody_andSetsHeaders() throws Exception {
      
        String baseUrl = "https://api.example.com";
        OrganizationAPICall svc = setupServiceWithUrl(baseUrl);

        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);

      
        httpClientBuilderStatic = mockStatic(HttpClientBuilder.class);
        HttpClientBuilder builder = mockBuilder();

        httpClientBuilderStatic.when(HttpClientBuilder::create).thenReturn(builder);
        when(builder.setDefaultRequestConfig(any(RequestConfig.class))).thenReturn(builder);
        when(builder.build()).thenReturn(httpClient);

       
        String body = "{\"ok\":true}";
        HttpEntity entity = new ByteArrayEntity(body.getBytes(StandardCharsets.UTF_8));
        when(response.getEntity()).thenReturn(entity);
        when(httpClient.execute(any(HttpGet.class))).thenReturn(response);

       
        String result = svc.getOrganization("org-123", "Bearer abc");

       
        assertThat(result).isEqualTo(body);

        
        ArgumentCaptor<HttpGet> captor = ArgumentCaptor.forClass(HttpGet.class);
        verify(httpClient).execute(captor.capture());
        HttpGet actual = captor.getValue();

        assertThat(actual.getURI().toString())
                .isEqualTo("https://api.example.com/my-organization");
        assertThat(actual.getFirstHeader("Authorization").getValue())
                .isEqualTo("Bearer abc");
        assertThat(actual.getFirstHeader("Content-Type").getValue())
                .isEqualTo("application/json");
        assertThat(actual.getFirstHeader("X-Org-Id").getValue())
                .isEqualTo("org-123");

        verify(response, times(1)).getEntity();
        verify(response, times(1)).close();
        verify(httpClient, times(1)).close();
    }

    @Test
    void getOrganization_httpExecuteThrows_returnsEmptyString() throws Exception {
       
        OrganizationAPICall svc = setupServiceWithUrl("https://api.example.com");

        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);

        httpClientBuilderStatic = mockStatic(HttpClientBuilder.class);
        HttpClientBuilder builder = mockBuilder();
        httpClientBuilderStatic.when(HttpClientBuilder::create).thenReturn(builder);
        when(builder.setDefaultRequestConfig(any(RequestConfig.class))).thenReturn(builder);
        when(builder.build()).thenReturn(httpClient);

        when(httpClient.execute(any(HttpGet.class))).thenThrow(new IOException("boom"));

      
        String result = svc.getOrganization("org-123", "Bearer abc");

       
        assertThat(result).isEmpty();
        verify(httpClient, times(1)).close();
    }

    @Test
    void getOrganization_nullEntity_returnsEmptyString() throws Exception {
       
        OrganizationAPICall svc = setupServiceWithUrl("https://api.example.com");

        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);

        httpClientBuilderStatic = mockStatic(HttpClientBuilder.class);
        HttpClientBuilder builder = mockBuilder();
        httpClientBuilderStatic.when(HttpClientBuilder::create).thenReturn(builder);
        when(builder.setDefaultRequestConfig(any(RequestConfig.class))).thenReturn(builder);
        when(builder.build()).thenReturn(httpClient);

        
        when(response.getEntity()).thenReturn(null);
        when(httpClient.execute(any(HttpGet.class))).thenReturn(response);

        
        String result = svc.getOrganization("org-123", "Bearer abc");

        assertThat(result).isEmpty();

        verify(response, times(1)).getEntity();
        verify(response, times(1)).close();
        verify(httpClient, times(1)).close();
    }
}
