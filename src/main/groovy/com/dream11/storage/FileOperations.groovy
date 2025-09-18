package com.dream11.storage

import com.dream11.spec.FileDownloadSpec

interface FileOperations {
    void download(FileDownloadSpec fileDownloadSpec, String workingDirectory)
}
