package com.dream11.exec

import com.dream11.spec.FileDownloadSpec
import com.dream11.storage.FileOperations
import com.dream11.storage.S3FileOperations
import com.dream11.storage.StorageProvider
import groovy.util.logging.Slf4j

@Slf4j
class FileCommandExecutor {
    static CommandResponse download(FileDownloadSpec fileDownloadSpec, String workingDirectory) {
        log.debug("Downloading file from [${fileDownloadSpec.getUri()}], working dir [${workingDirectory}]")
        FileOperations fileOperations
        switch (StorageProvider.valueOf(fileDownloadSpec.getProvider())) {
            case StorageProvider.S3:
                fileOperations = new S3FileOperations()
                break
            default:
                throw new IllegalArgumentException("Unsupported provider ${fileDownloadSpec.getProvider()}")
        }
        try {
            fileOperations.download(fileDownloadSpec, workingDirectory)
            return new CommandResponse(null, new StringBuilder()
                    .append("Downloaded file from ${fileDownloadSpec.getUri()}").toString(),
                    null, 0)
        }
        catch (Exception e) {
            log.error("Failed to download ${fileDownloadSpec.getUri()}", e)
            return new CommandResponse(null, null, new StringBuilder()
                    .append(e.getMessage()).toString(), 1)
        }

    }
}
