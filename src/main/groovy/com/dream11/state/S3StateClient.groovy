package com.dream11.state


import groovy.util.logging.Slf4j
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.core.retry.RetryMode
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3ClientBuilder
import software.amazon.awssdk.services.s3.S3Uri
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import software.amazon.awssdk.services.s3.model.NoSuchKeyException

@Slf4j
class S3StateClient implements StateClient {

    S3StateClientConfig stateConfig
    S3Client s3Client

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
        // Log complete config for debugging
        log.debug("S3StateClient initialized with config: ${this.stateConfig}, retry strategy: STANDARD")
    }

    @Override
    String getState() {
        // Parse and validate S3 URI using centralized utility
        S3Uri s3Uri = S3Utils.parseAndValidateS3Uri(stateConfig.getUri())
        String bucket = s3Uri.bucket().get()
        String key = s3Uri.key().get()

        if (S3Utils.isDirectory(s3Uri)) {
            log.error("Given S3 uri [${stateConfig.getUri()}] is a directory")
            throw new IllegalArgumentException("Given S3 uri [${stateConfig.getUri()}] is a directory")
        } else {
            log.debug("Given S3 uri [${stateConfig.getUri()}] is a file")

            try {
                // Check if object exists using HeadObject with Consumer pattern
                s3Client.headObject(request -> request.bucket(bucket).key(key))

                // Object exists, download it using Consumer pattern
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
        // Parse and validate S3 URI using centralized utility
        S3Uri s3Uri = S3Utils.parseAndValidateS3Uri(stateConfig.getUri())
        String bucket = s3Uri.bucket().get()
        String key = s3Uri.key().get()

        // Check if the bucket exists; if not, create it using Consumer pattern
        try {
            s3Client.headBucket(request -> request.bucket(bucket))
        } catch (NoSuchBucketException ignored) {
            s3Client.createBucket(request -> request.bucket(bucket))
        }

        // Read the content from a local file in the working directory
        File localFile = new File(workingDirectory, "odin.state")

        // Upload the state file to S3 using Consumer pattern
        // ensures that the state file is either fully uploaded or not uploaded at all
        s3Client.putObject(request -> request.bucket(bucket).key(key), RequestBody.fromFile(localFile))
    }

    @Override
    void deleteState() {
        // Parse and validate S3 URI using centralized utility
        S3Uri s3Uri = S3Utils.parseAndValidateS3Uri(stateConfig.getUri())
        String bucket = s3Uri.bucket().get()
        String key = s3Uri.key().get()

        // Check if the bucket exists; if not, log error and return using Consumer pattern
        try {
            s3Client.headBucket(request -> request.bucket(bucket))
        } catch (NoSuchBucketException ignored) {
            log.error("Unable to delete state. Bucket does not exist: ${bucket}")
            return
        }

        // Delete the state file from S3 using Consumer pattern
        s3Client.deleteObject(request -> request.bucket(bucket).key(key))
    }
}
