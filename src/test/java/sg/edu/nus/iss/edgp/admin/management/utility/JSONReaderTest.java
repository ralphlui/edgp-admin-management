package sg.edu.nus.iss.edgp.admin.management.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import sg.edu.nus.iss.edgp.admin.management.api.connector.NotificationAPICall;
import sg.edu.nus.iss.edgp.admin.management.api.connector.OrganizationAPICall;
import sg.edu.nus.iss.edgp.admin.management.entity.UserInvitation;

@ExtendWith(MockitoExtension.class)
class JSONReaderTest {

    @Mock
    private NotificationAPICall notiAPICall;

    @Mock
    private OrganizationAPICall orgAPICall;

    @InjectMocks
    private JSONReader jsonReader;

    private JSONObject sampleResponse;

    @BeforeEach
    void setUp() {
        sampleResponse = new JSONObject();
        sampleResponse.put("success", true);
        sampleResponse.put("message", "Operation successful");
        sampleResponse.put("status", 200L);

        JSONObject data = new JSONObject();
        data.put("name", "Test Org");
        sampleResponse.put("data", data);
    }
    
    @Test
    void testParseJsonResponse_valid() throws Exception {
        String jsonStr = sampleResponse.toJSONString();
        JSONObject result = jsonReader.parseJsonResponse(jsonStr);
        assertEquals(sampleResponse, result);
    }

    @Test
    void testParseJsonResponse_nullOrEmpty() throws Exception {
        assertNull(jsonReader.parseJsonResponse(null));
        assertNull(jsonReader.parseJsonResponse(""));
    }
    
    @Test
    void testGetDataFromResponse() {
        JSONObject result = jsonReader.getDataFromResponse(sampleResponse);
        assertNotNull(result);
        assertEquals("Test Org", result.get("name"));
    }

    @Test
    void testGetDataFromResponse_nullOrEmpty() {
        assertNull(jsonReader.getDataFromResponse(null));
        assertNull(jsonReader.getDataFromResponse(new JSONObject()));
    }
    
    @Test
    void testGetMessageFromResponse() {
        String message = jsonReader.getMessageFromResponse(sampleResponse);
        assertEquals("Operation successful", message);
    }

    @Test
    void testGetSuccessFromResponse() {
        Boolean success = jsonReader.getSuccessFromResponse(sampleResponse);
        assertTrue(success);
    }
    
    @Test
    void testGetStatusFromResponse() {
        int status = jsonReader.getStatusFromResponse(sampleResponse);
        assertEquals(200, status);
    }

    @Test
    void testSendUserInviteEmail_success() {
        UserInvitation invitation = new UserInvitation();
        String header = "Bearer token";
        String jsonStr = sampleResponse.toJSONString();

        when(notiAPICall.sendUserInviteEmail(invitation, header)).thenReturn(jsonStr);

        JSONObject result = jsonReader.sendUserInviteEmail(invitation, header);

        assertNotNull(result);
        assertEquals(true, result.get("success"));
        verify(notiAPICall).sendUserInviteEmail(invitation, header);
    }


    @Test
    void testGetOrganization_success() {
        String orgId = "org123";
        String header = "Bearer token";
        String jsonStr = sampleResponse.toJSONString();

        when(orgAPICall.getOrganization(orgId, header)).thenReturn(jsonStr);

        JSONObject result = jsonReader.getOrganization(orgId, header);

        assertNotNull(result);
        assertEquals("Operation successful", result.get("message"));
        verify(orgAPICall).getOrganization(orgId, header);
    }
}


