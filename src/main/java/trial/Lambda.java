package main.java.trial;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.event.S3EventNotification.S3EventNotificationRecord;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishResult;

import java.util.stream.Collectors;

public class Lambda implements RequestHandler<S3Event, String> {
    private static final String DELETED = "ObjectRemoved";
    private static final String CREATED = "ObjectCreated";
    private static final String SNS_TOPIC = "arn:aws:sns:us-east-1:313027212265:test";

    @Override
    public String handleRequest(S3Event event, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Received event count: " + event.getRecords().size());
        AmazonSNS client = AmazonSNSClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();
        String message = String.join("\n", event.getRecords().stream().map(this::getNotificationString).collect(Collectors.toList()));
        PublishResult publishResult = client.publish(SNS_TOPIC, message);
        return publishResult.getMessageId();
    }

    private String getNotificationString(S3EventNotificationRecord s3EventNotificationRecord) {
        StringBuilder sb = new StringBuilder("\n");
        sb.append("Region: ").append(s3EventNotificationRecord.getAwsRegion()).append("\n");
        sb.append("User: ").append(s3EventNotificationRecord.getUserIdentity().getPrincipalId()).append("\n");
        sb.append("Bucket: ").append(s3EventNotificationRecord.getS3().getBucket().getName()).append("\n");
        sb.append("Key: ").append(s3EventNotificationRecord.getS3().getObject().getKey()).append("\n");

        String eventName = s3EventNotificationRecord.getEventName();
        if (eventName.startsWith(DELETED)) {
            sb.append("This object is deleted!");
        } else if (eventName.startsWith(CREATED)) {
            sb.append("A new object is uploaded!");
        } else {
            sb.append("Unknown event: ").append(eventName);
        }
        return sb.toString();
    }

}
