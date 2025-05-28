package sg.edu.nus.iss.edgp.admin.management.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

@Configuration
public class AWSConfig {
	
	@Value("${spring.cloud.aws.region.static}")
	private String awsRegion;

	@Value("${spring.cloud.aws.credentials.access-key}")
	private String awsAccessKey;

	@Value("${spring.cloud.aws.credentials.secret-key}")
	private String awsSecretKey;
	
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
	public AWSCredentials awsCredentials() {
		return new BasicAWSCredentials(awsAccessKey, awsSecretKey);
	}
	
	
	@Bean
    public AmazonSQS amazonSQSClient(AWSCredentials awsCredentials) {
        return AmazonSQSClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(awsRegion)
                .build();
    }

}
