package sg.edu.nus.iss.edgp.admin.management.configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
public class AWSConfig {

	private static final Logger LOG = LogManager.getLogger(AWSConfig.class);

	@Value("${spring.cloud.aws.region.static}")
	private String awsRegion;

	@Value("${spring.cloud.aws.credentials.access-key}")
	private String awsAccessKey;

	@Value("${spring.cloud.aws.credentials.secret-key}")
	private String awsSecretKey;

	@Value("${aws.sqs.queue.audit.url}")
	private String sqsURL;

	@Bean
	public String getAwsRegion() {
		return awsRegion;
	}

	@Bean
	public String getAwsAccessKey() {
		return awsAccessKey;
	}

	@Bean
	public String getAwsSecretKey() {
		return awsSecretKey;
	}

	@Bean
	public String getSQSUrl() {
		return sqsURL;
	}

	@Bean
	public SqsClient sqsClient() {
		return SqsClient.builder().region(Region.AP_SOUTHEAST_1).build();
	}
	
	@Bean
	public DynamoDbClient dynamoDbClient() {
	    return DynamoDbClient.builder()
	            .region(Region.AP_SOUTHEAST_1)
	            .build();
	}

}
