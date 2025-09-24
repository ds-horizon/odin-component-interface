package com.dream11.storage

import com.dream11.OdinUtil
import com.dream11.S3Util
import com.dream11.spec.FileDownloadSpec
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration
import software.amazon.awssdk.core.retry.RetryMode
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Uri
import software.amazon.awssdk.services.s3.S3Utilities
import software.amazon.awssdk.transfer.s3.S3TransferManager
import software.amazon.awssdk.transfer.s3.model.CompletedDirectoryDownload
import software.amazon.awssdk.transfer.s3.model.CompletedFileDownload
import software.amazon.awssdk.transfer.s3.model.DirectoryDownload
import software.amazon.awssdk.transfer.s3.model.DownloadDirectoryRequest
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest
import software.amazon.awssdk.transfer.s3.model.FailedFileDownload
import software.amazon.awssdk.transfer.s3.model.FileDownload

import java.nio.file.Paths

@Slf4j
class S3FileOperations implements FileOperations {

    private final S3AsyncClient s3AsyncClient
    private final S3TransferManager transferManager
    private volatile boolean closed = false
    private final S3Utilities s3Utilities

    S3FileOperations(String region) {
        // Configure retry strategy with standard mode (3 retries by default)
        ClientOverrideConfiguration overrideConfig = ClientOverrideConfiguration.builder()
                .retryStrategy(RetryMode.STANDARD)
                .build()

        // Initialize with S3 async client with retry configuration
        // Even though we use blocking operations (.join()), Transfer Manager needs async client
        this.s3AsyncClient = S3AsyncClient.builder()
                .overrideConfiguration(overrideConfig)
                .region(Region.of(region))
                .build()

        this.transferManager = S3TransferManager.builder()
                .s3Client(s3AsyncClient)
                .build()

        this.s3Utilities = S3Utilities.builder()
                .region(Region.of(region))
                .build()
        // Register shutdown hook to ensure resources are cleaned up
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.debug("Shutting down S3FileOperations resources")
            this.close()
        }))
    }

    @Override
    void download(FileDownloadSpec fileDownloadSpec, String workingDirectory) {
        S3Uri s3Uri = S3Util.parseAndValidateS3Uri(fileDownloadSpec.getUri(), this.s3Utilities)
        String bucket = s3Uri.bucket().get()
        String key = s3Uri.key().get()

        if (S3Util.isDirectory(s3Uri)) {
            log.debug("Given S3 uri [${fileDownloadSpec.getUri()}] is a directory")
            downloadDirectory(bucket, key, fileDownloadSpec.getRelativeDestination(), workingDirectory)
        } else {
            log.debug("Given S3 uri [${fileDownloadSpec.getUri()}] is a file")
            downloadFile(bucket, key, fileDownloadSpec.getRelativeDestination(), workingDirectory)
        }
    }

    private void downloadDirectory(String bucket, String key, String relativeDestination, String workingDirectory) {
        String tempDirPath = OdinUtil.joinPath(workingDirectory, "tmp")

        // Build the download directory request
        DownloadDirectoryRequest downloadDirectoryRequest = DownloadDirectoryRequest.builder()
                .destination(Paths.get(tempDirPath))
                .bucket(bucket)
                .listObjectsV2RequestTransformer(request -> request.prefix(key))
                .build()

        DirectoryDownload directoryDownload = transferManager.downloadDirectory(downloadDirectoryRequest)
        CompletedDirectoryDownload completedDirectoryDownload = directoryDownload.completionFuture().join()

        // Check for failed transfers with detailed error information
        if (!completedDirectoryDownload.failedTransfers().isEmpty()) {
            log.error("Failed to download ${completedDirectoryDownload.failedTransfers().size()} files from S3")

            List<String> failureDetails = []
            completedDirectoryDownload.failedTransfers().each { FailedFileDownload failedDownload ->
                String failureMessage = "Failed: ${failedDownload.request().getObjectRequest().key()} - ${failedDownload.exception().getMessage()}"
                log.error(failureMessage)
                failureDetails.add(failureMessage)
            }

            throw new RuntimeException("Directory download completed with ${completedDirectoryDownload.failedTransfers().size()} failures. Details: ${failureDetails.join('; ')}")
        }

        File downloadDirectory = new File(OdinUtil.joinPath(tempDirPath, key))
        File finalDestinationDirectory = new File(OdinUtil.joinPath(workingDirectory, relativeDestination == null ? "" : relativeDestination))
        FileUtils.copyDirectory(downloadDirectory, finalDestinationDirectory)
        FileUtils.deleteDirectory(new File(tempDirPath))
    }

    private void downloadFile(String bucket, String key, String relativeDestination, String workingDirectory) {
        File downloadFile = new File(OdinUtil.joinPath(workingDirectory, relativeDestination == null ? key.split("/").last() : relativeDestination))

        // Build the download file request
        DownloadFileRequest downloadFileRequest = DownloadFileRequest.builder()
                .getObjectRequest(req -> req.bucket(bucket).key(key))
                .destination(downloadFile.toPath())
                .build()

        FileDownload fileDownload = transferManager.downloadFile(downloadFileRequest)
        CompletedFileDownload completedFileDownload = fileDownload.completionFuture().join()

        log.debug("Downloaded file from S3: ${completedFileDownload.response().responseMetadata()}")
    }

    /**
     * Closes the transfer manager and S3 client to release resources.
     * This method is thread-safe and idempotent.
     */
    synchronized void close() {
        if (closed) {
            log.debug("S3FileOperations already closed")
            return
        }

        try {
            log.debug("Closing S3TransferManager")
            transferManager.close()
        } catch (Exception e) {
            log.warn("Error closing TransferManager", e)
        } finally {
            try {
                // S3AsyncClient must be closed separately as per AWS SDK documentation
                log.debug("Closing S3AsyncClient")
                s3AsyncClient.close()
            } catch (Exception e) {
                log.warn("Error closing S3AsyncClient", e)
            }
            closed = true
        }
    }
}
