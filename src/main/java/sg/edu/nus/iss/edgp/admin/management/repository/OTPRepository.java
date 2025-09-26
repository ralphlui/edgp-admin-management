package sg.edu.nus.iss.edgp.admin.management.repository;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import sg.edu.nus.iss.edgp.admin.management.dto.OTPItemDTO;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

@Repository
@RequiredArgsConstructor
public class OTPRepository {
 private static final Logger log = LoggerFactory.getLogger(OTPRepository.class);

 private final DynamoDbClient dynamoDbClient;


 @Value("${aws.dynamodb.table.otp}")
	private String tableName;
 

 public void put(OTPItemDTO item) {
     PutItemRequest req = PutItemRequest.builder()
         .tableName(tableName)
         .item(Map.of(
             "email",        AttributeValue.builder().s(item.getPk()).build(),
             "otp",       AttributeValue.builder().s(item.getOtp()).build(),
             "expiresAt", AttributeValue.builder().n(Long.toString(item.getExpiresAt())).build(),
             "createdAt", AttributeValue.builder().n(Long.toString(item.getCreatedAt())).build()
         ))
         // Upsert : generateOTP should overwrite any existing OTP for this user
         .build();

     dynamoDbClient.putItem(req);
 }

 public OTPItemDTO get(String pk) {
     GetItemRequest req = GetItemRequest.builder()
         .tableName(tableName)
         .key(Map.of("email", AttributeValue.builder().s(pk).build()))
         .consistentRead(true)
         .build();

     var resp = dynamoDbClient.getItem(req);
     if (resp.item() == null || resp.item().isEmpty()) return null;

     var m = resp.item();
     return OTPItemDTO.builder()
         .pk(m.get("email").s())
         .otp(m.get("otp").s())
         .expiresAt(Long.parseLong(m.get("expiresAt").n()))
         .createdAt(Long.parseLong(m.get("createdAt").n()))
         .build();
 }

 
 public boolean deleteIfMatch(String pk, String otp) {
     DeleteItemRequest req = DeleteItemRequest.builder()
         .tableName(tableName)
         .key(Map.of("email", AttributeValue.builder().s(pk).build()))
         .conditionExpression("otp = :otp")
         .expressionAttributeValues(Map.of(
             ":otp", AttributeValue.builder().s(otp).build()
         ))
         .build();

     try {
         dynamoDbClient.deleteItem(req);
         return true;
     } catch (ConditionalCheckFailedException e) {
         log.debug("Conditional delete failed for pk={}", pk);
         return false;
     }
 }
}
