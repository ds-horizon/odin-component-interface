package com.dream11.state

import com.dream11.S3Util
import groovy.util.logging.Slf4j
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.core.retry.RetryMode
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3ClientBuilder
import software.amazon.awssdk.services.s3.S3Uri
import software.amazon.awssdk.services.s3.S3Utilities
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

@Slf4j
class S3StateClient implements StateClient {

    private final S3StateClientConfig stateConfig
    private final S3Client s3Client
    private final S3Utilities s3Utilities

    S3StateClient(StateClientConfig stateConfig) {
        this.stateConfig = (S3StateClientConfig) stateConfig

        // Configure retry strategy with standard mode (3 retries by default)
        ClientOverrideConfiguration overrideConfig = ClientOverrideConfiguration.builder()
                .retryStrategy(RetryMode.STANDARD)
                .build()

        S3ClientBuilder clientBuilder = S3Client.builder()
                .overrideConfiguration(overrideConfig)

        if (this.stateConfig.getEndpoint() != null && !this.stateConfig.getEndpoint().isEmpty()) {
            // Use custom endpoint if provided
            clientBuilder.endpointOverride(URI.create(this.stateConfig.getEndpoint()))
                    .region(Region.of(this.stateConfig.getRegion()))
        } else {
            // Use default AWS S3 endpoint for the region
            clientBuilder.region(Region.of(this.stateConfig.getRegion()))
        }

        this.s3Client = clientBuilder.build()
        this.s3Utilities = S3Utilities.builder()
                .region(Region.of(this.stateConfig.getRegion()))
                .build()
        // Log complete config for debugging
        log.debug("S3StateClient initialized with config: ${this.stateConfig}, retry strategy: STANDARD")
    }

    @Override
    String getState() {
        S3Uri s3Uri = S3Util.parseAndValidateS3Uri(stateConfig.getUri(), this.s3Utilities)
        String bucket = s3Uri.bucket().get()
        String key = s3Uri.key().get()

        if (S3Util.isDirectory(s3Uri)) {
            log.error("Given S3 uri [${stateConfig.getUri()}] is a directory")
            throw new IllegalArgumentException("Given S3 uri [${stateConfig.getUri()}] is a directory")
        } else {
            log.debug("Given S3 uri [${stateConfig.getUri()}] is a file")

            try {
                // Check if object exists using HeadObject
                s3Client.headObject(request -> request.bucket(bucket).key(key))

                // Object exists, download it
                ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request ->
                        request.bucket(bucket).key(key)
                )
                return response.asUtf8String()
            } catch (NoSuchKeyException ignored) {
                log.debug("Object does not exist: ${stateConfig.getUri()}")
                return ""
            }
        }
    }

    @Override
    void putState(String workingDirectory) {
        S3Uri s3Uri = S3Util.parseAndValidateS3Uri(stateConfig.getUri(), this.s3Utilities)
        String bucket = s3Uri.bucket().get()
        String key = s3Uri.key().get()

        // Create bucket on the fly it it doesn't exist
        try {
            s3Client.headBucket(request -> request.bucket(bucket))
        } catch (NoSuchBucketException ignored) {
            s3Client.createBucket(request -> request.bucket(bucket))
        }

        // Read the content from a local file in the working directory
        File localFile = new File(workingDirectory, "odin.state")

        // Upload the state file to S3
        // ensures that the state file is either fully uploaded or not uploaded at all
        s3Client.putObject(request -> request.bucket(bucket).key(key), RequestBody.fromFile(localFile))
    }

    @Override
    void deleteState() {
        S3Uri s3Uri = S3Util.parseAndValidateS3Uri(stateConfig.getUri(), this.s3Utilities)
        String bucket = s3Uri.bucket().get()
        String key = s3Uri.key().get()

        // if the bucket does not exists, simply return since there is no need to delete
        try {
            s3Client.headBucket(request -> request.bucket(bucket))
        } catch (NoSuchBucketException ignored) {
            log.error("Unable to delete state. Bucket does not exist: ${bucket}")
            return
        }

        // Delete the state file from S3
        s3Client.deleteObject(request -> request.bucket(bucket).key(key))
    }
}
