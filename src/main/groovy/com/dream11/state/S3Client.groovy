package com.dream11.state

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.AmazonS3URI
import com.amazonaws.services.s3.model.S3Object
import groovy.util.logging.Slf4j

@Slf4j
class S3Client implements StateClient{

    StateConfig stateConfig
    AmazonS3 s3Client

    S3Client(StateConfig stateConfig) {
        this.s3Client = AmazonS3Client.builder().build()
        this.stateConfig = stateConfig
    }

    @Override
    String getState() {
        AmazonS3URI s3Uri = new AmazonS3URI(stateConfig.getUri())
        String bucket = s3Uri.getBucket()
        String key = s3Uri.getKey()

        if (key.endsWith("/")) {
            log.error("Given S3 uri [${stateConfig.getUri()}] is a directory")
            throw new IllegalArgumentException("Given S3 uri [${stateConfig.getUri()}] is a directory")
        } else {
            log.debug("Given S3 uri [${stateConfig.getUri()}] is a file")
            // Download the object using Amazon S3's built-in strong consistency

            if(s3Client.doesObjectExist(bucket, key)) {
                S3Object s3Object = s3Client.getObject(bucket, key)
                InputStream objectContent = s3Object.getObjectContent()
                def fileContents = objectContent.text // Read content into a string

                // Close the S3 object to release resources
                s3Object.close()
                return fileContents
            } else {
                log.debug("Object does not exist: ${stateConfig.getUri()}")
                return ""
            }
        }
    }


    @Override
    void putState(String workingDirectory) {
        AmazonS3URI s3Uri = new AmazonS3URI(stateConfig.getUri())
        String bucket = s3Uri.getBucket()
        String key = s3Uri.getKey()

        // Check if the bucket exists; if not, create it
        if (!s3Client.doesBucketExistV2(bucket)) {
            s3Client.createBucket(bucket)
        }

        // Read the content from a local file in the working directory
        File localFile = new File(workingDirectory, "odin.state")

        // Upload the state file to S3, synchronous operation and consistency guarantee
        // ensures that the state file is either fully uploaded or not uploaded at all
        s3Client.putObject(bucket, key, localFile)
    }

    @Override
    void deleteState(){
        AmazonS3URI s3Uri = new AmazonS3URI(stateConfig.getUri())
        String bucket = s3Uri.getBucket()
        String key = s3Uri.getKey()

        // Check if the bucket exists; if not, throw an exception
        if (!s3Client.doesBucketExistV2(bucket)) {
            log.error("Unable to delete state. Bucket does not exist: ${bucket}")
            return
        }

        // Delete the state file to S3,
        s3Client.deleteObject(bucket, key)
    }
}
