package com.dream11.storage

import com.amazonaws.services.s3.AmazonS3URI
import com.amazonaws.services.s3.transfer.Download
import com.amazonaws.services.s3.transfer.MultipleFileDownload
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.transfer.TransferManagerBuilder
import com.dream11.OdinUtil
import com.dream11.spec.FileDownloadSpec
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils

@Slf4j
class S3FileOperations implements FileOperations {

    TransferManager transferManager = TransferManagerBuilder.standard().build()

    @Override
    void download(FileDownloadSpec fileDownloadSpec, String workingDirectory) {
        AmazonS3URI s3Uri = new AmazonS3URI(fileDownloadSpec.getUri())
        String bucket = s3Uri.getBucket()
        String key = s3Uri.getKey()

        if (key.endsWith("/")) {
            log.debug("Given S3 uri [${fileDownloadSpec.getUri()}] is a directory")
            downloadDirectory(bucket, key, fileDownloadSpec.getRelativeDestination(), workingDirectory)
        } else {
            log.debug("Given S3 uri [${fileDownloadSpec.getUri()}] is a file")
            downloadFile(bucket, key, fileDownloadSpec.getRelativeDestination(), workingDirectory)
        }

        transferManager.shutdownNow()
    }

    private void downloadDirectory(String bucket, String key, String relativeDestination, String workingDirectory) {
        String tempDirPath = OdinUtil.joinPath(workingDirectory, "tmp")
        MultipleFileDownload s3Object = transferManager.downloadDirectory(bucket, key, new File(tempDirPath))
        s3Object.waitForCompletion()

        File downloadDirectory = new File(OdinUtil.joinPath(tempDirPath, key))
        File finalDestinationDirectory = new File(OdinUtil.joinPath(workingDirectory, relativeDestination == null ? "" : relativeDestination))
        FileUtils.copyDirectory(downloadDirectory, finalDestinationDirectory)
        FileUtils.deleteDirectory(new File(tempDirPath))
    }

    private void downloadFile(String bucket, String key, String relativeDestination, String workingDirectory) {
        File downloadFile = new File(OdinUtil.joinPath(workingDirectory, relativeDestination == null ? key.split("/").last() : relativeDestination))
        Download s3Object = transferManager.download(bucket, key, downloadFile)
        s3Object.waitForCompletion()
    }
}
