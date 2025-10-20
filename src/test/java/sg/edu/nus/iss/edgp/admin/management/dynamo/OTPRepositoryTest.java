package sg.edu.nus.iss.edgp.admin.management.dynamo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import sg.edu.nus.iss.edgp.admin.management.dto.OTPItemDTO;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;
import software.amazon.awssdk.services.dynamodb.model.TableStatus;

@ExtendWith(MockitoExtension.class)
class OTPRepositoryTest {

    private DynamoDbClient dynamo;
    private OTPRepository repo;

    @BeforeEach
    void setup() {
        dynamo = mock(DynamoDbClient.class, RETURNS_DEEP_STUBS);
        repo = new OTPRepository(dynamo);
        
        ReflectionTestUtils.setField(repo, "tableName", "otp_table");
    }

    private DescribeTableResponse activeTable() {
        return DescribeTableResponse.builder()
            .table(TableDescription.builder().tableStatus(TableStatus.ACTIVE).build())
            .build();
    }

    

    @Test
    void tableExists_true_whenDescribeSucceeds() {
        when(dynamo.describeTable(any(DescribeTableRequest.class))).thenReturn(activeTable());

        boolean exists = repo.tableExists("otp_table");

        assertThat(exists).isTrue();
        verify(dynamo).describeTable(any(DescribeTableRequest.class));
    }

    @Test
    void tableExists_false_whenDescribeThrowsNotFound() {
        when(dynamo.describeTable(any(DescribeTableRequest.class)))
            .thenThrow(ResourceNotFoundException.builder().message("not found").build());

        boolean exists = repo.tableExists("otp_table");

        assertThat(exists).isFalse();
    }

   

    @Test
    void createTable_buildsRequest_andWaitsUntilActive() {
     
        when(dynamo.describeTable(any(DescribeTableRequest.class))).thenReturn(activeTable());

        repo.createTable("otp_table", "email");

        ArgumentCaptor<CreateTableRequest> cap = ArgumentCaptor.forClass(CreateTableRequest.class);
        verify(dynamo).createTable(cap.capture());
        CreateTableRequest req = cap.getValue();

        assertThat(req.tableName()).isEqualTo("otp_table");
        assertThat(req.keySchema()).hasSize(1);
        assertThat(req.keySchema().get(0).attributeName()).isEqualTo("email");
        assertThat(req.attributeDefinitions()).hasSize(1);
        assertThat(req.attributeDefinitions().get(0).attributeName()).isEqualTo("email");

        verify(dynamo, atLeastOnce()).describeTable(any(DescribeTableRequest.class));
    }

 

    @Test
    void put_createsTableIfNotExists_thenWritesItem() {
        
        when(dynamo.describeTable(any(DescribeTableRequest.class)))
            .thenThrow(ResourceNotFoundException.builder().message("nf").build())
            
            .thenReturn(activeTable());

        OTPItemDTO item = OTPItemDTO.builder()
            .pk("user@example.com")
            .otp("123456")
            .expiresAt(1700000000000L)
            .createdAt(1699990000000L)
            .build();

        repo.put(item);

        
        verify(dynamo).createTable(any(CreateTableRequest.class));

        
        ArgumentCaptor<PutItemRequest> putCap = ArgumentCaptor.forClass(PutItemRequest.class);
        verify(dynamo).putItem(putCap.capture());
        PutItemRequest putReq = putCap.getValue();

        assertThat(putReq.tableName()).isEqualTo("otp_table");
        Map<String, AttributeValue> m = putReq.item();
        assertThat(m.get("email").s()).isEqualTo("user@example.com");
        assertThat(m.get("otp").s()).isEqualTo("123456");
        assertThat(m.get("expiresAt").n()).isEqualTo("1700000000000");
        assertThat(m.get("createdAt").n()).isEqualTo("1699990000000");
    }

    @Test
    void put_whenTableAlreadyExists_onlyWritesItem() {
    
        when(dynamo.describeTable(any(DescribeTableRequest.class))).thenReturn(activeTable());

        OTPItemDTO item = OTPItemDTO.builder()
            .pk("user@example.com").otp("999999")
            .expiresAt(1L).createdAt(2L).build();

        repo.put(item);

        
        verify(dynamo, never()).createTable(any(CreateTableRequest.class));

        
        verify(dynamo).putItem(any(PutItemRequest.class));
    }

    

    @Test
    void get_returnsDTO_whenItemFound() {
        Map<String, AttributeValue> stored = Map.of(
            "email", AttributeValue.builder().s("user@example.com").build(),
            "otp", AttributeValue.builder().s("654321").build(),
            "expiresAt", AttributeValue.builder().n("1700000000001").build(),
            "createdAt", AttributeValue.builder().n("1699990000001").build()
        );

        when(dynamo.getItem(any(GetItemRequest.class)))
            .thenReturn(GetItemResponse.builder().item(stored).build());

        OTPItemDTO dto = repo.get("user@example.com");

        assertThat(dto).isNotNull();
        assertThat(dto.getPk()).isEqualTo("user@example.com");
        assertThat(dto.getOtp()).isEqualTo("654321");
        assertThat(dto.getExpiresAt()).isEqualTo(1700000000001L);
        assertThat(dto.getCreatedAt()).isEqualTo(1699990000001L);

        ArgumentCaptor<GetItemRequest> getCap = ArgumentCaptor.forClass(GetItemRequest.class);
        verify(dynamo).getItem(getCap.capture());
        assertThat(getCap.getValue().key().get("email").s()).isEqualTo("user@example.com");
    }

    @Test
    void get_returnsNull_whenItemMissing() {
        when(dynamo.getItem(any(GetItemRequest.class)))
            .thenReturn(GetItemResponse.builder().build()); // no item

        OTPItemDTO dto = repo.get("missing@example.com");
        assertThat(dto).isNull();
    }

     

    @Test
    void deleteIfMatch_returnsTrue_onSuccess() {
       
        boolean ok = repo.deleteIfMatch("user@example.com", "123456");
        assertThat(ok).isTrue();

        ArgumentCaptor<DeleteItemRequest> delCap = ArgumentCaptor.forClass(DeleteItemRequest.class);
        verify(dynamo).deleteItem(delCap.capture());
        DeleteItemRequest req = delCap.getValue();

        assertThat(req.tableName()).isEqualTo("otp_table");
        assertThat(req.key().get("email").s()).isEqualTo("user@example.com");
        assertThat(req.conditionExpression()).isEqualTo("otp = :otp");
        assertThat(req.expressionAttributeValues().get(":otp").s()).isEqualTo("123456");
    }

    @Test
    void deleteIfMatch_returnsFalse_onConditionalCheckFailed() {
        doThrow(ConditionalCheckFailedException.builder().message("bad otp").build())
            .when(dynamo).deleteItem(any(DeleteItemRequest.class));

        boolean ok = repo.deleteIfMatch("user@example.com", "wrong");
        assertThat(ok).isFalse();
    }
}
