package sg.edu.nus.iss.edgp.admin.management.repository;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import sg.edu.nus.iss.edgp.admin.management.dto.OTPItemDTO;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

@Repository
@RequiredArgsConstructor
public class OTPRepository {
	private static final Logger log = LoggerFactory.getLogger(OTPRepository.class);

	private final DynamoDbClient dynamoDbClient;

	@Value("${aws.dynamodb.table.otp}")
	private String tableName;

	public boolean tableExists(String tableName) {
		try {
			dynamoDbClient.describeTable(DescribeTableRequest.builder().tableName(tableName).build());
			return true;
		} catch (ResourceNotFoundException e) {
			return false;
		}
	}

	public void createTable(String tableName) {
		CreateTableRequest request = CreateTableRequest.builder().tableName(tableName)
				.keySchema(KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build())
				.attributeDefinitions(
						AttributeDefinition.builder().attributeName("id").attributeType(ScalarAttributeType.S).build())
				.billingMode(BillingMode.PAY_PER_REQUEST).build();

		dynamoDbClient.createTable(request);
		// Wait until table is ACTIVE
		waitForTableToBecomeActive(tableName);
	}

	private void waitForTableToBecomeActive(String tableName) {
		while (true) {
			DescribeTableResponse response = dynamoDbClient
					.describeTable(DescribeTableRequest.builder().tableName(tableName).build());

			String status = response.table().tableStatusAsString();
			if ("ACTIVE".equalsIgnoreCase(status))
				break;

			try {
				Thread.sleep(1000); // Wait 1 sec before checking again
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new RuntimeException("Interrupted while waiting for DynamoDB table to become active");
			}
		}
	}

	public void put(OTPItemDTO item) {

		// 1) Ensure tables exist, then save
		if (!this.tableExists(tableName.trim())) {
			this.createTable(tableName.trim());
		}

		PutItemRequest req = PutItemRequest.builder().tableName(tableName)
				.item(Map.of("email", AttributeValue.builder().s(item.getPk()).build(), "otp",
						AttributeValue.builder().s(item.getOtp()).build(), "expiresAt",
						AttributeValue.builder().n(Long.toString(item.getExpiresAt())).build(), "createdAt",
						AttributeValue.builder().n(Long.toString(item.getCreatedAt())).build()))
				// Upsert : generateOTP should overwrite any existing OTP for this user
				.build();

		dynamoDbClient.putItem(req);
	}

	public OTPItemDTO get(String pk) {
		GetItemRequest req = GetItemRequest.builder().tableName(tableName)
				.key(Map.of("email", AttributeValue.builder().s(pk).build())).consistentRead(true).build();

		var resp = dynamoDbClient.getItem(req);
		if (resp.item() == null || resp.item().isEmpty())
			return null;

		var m = resp.item();
		return OTPItemDTO.builder().pk(m.get("email").s()).otp(m.get("otp").s())
				.expiresAt(Long.parseLong(m.get("expiresAt").n())).createdAt(Long.parseLong(m.get("createdAt").n()))
				.build();
	}

	public boolean deleteIfMatch(String pk, String otp) {
		DeleteItemRequest req = DeleteItemRequest.builder().tableName(tableName)
				.key(Map.of("email", AttributeValue.builder().s(pk).build())).conditionExpression("otp = :otp")
				.expressionAttributeValues(Map.of(":otp", AttributeValue.builder().s(otp).build())).build();

		try {
			dynamoDbClient.deleteItem(req);
			return true;
		} catch (ConditionalCheckFailedException e) {
			log.debug("Conditional delete failed for pk={}", pk);
			return false;
		}
	}
}
