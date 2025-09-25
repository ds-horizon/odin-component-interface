package com.dream11

import software.amazon.awssdk.services.s3.S3Uri
import software.amazon.awssdk.services.s3.S3Utilities

/**
 * Utility class for S3 operations
 */
final class S3Util {

    private S3Util() {
    }

    static S3Uri parseAndValidateS3Uri(String uri, S3Utilities s3Utilities) {
        if (uri == null || uri.isEmpty()) {
            throw new IllegalArgumentException("S3 URI cannot be null or empty")
        }

        S3Uri s3Uri = s3Utilities.parseUri(URI.create(uri))

        if (!s3Uri.bucket().isPresent() || s3Uri.bucket().get().isEmpty()) {
            throw new IllegalArgumentException("Invalid S3 URI - missing bucket: ${uri}")
        }

        if (!s3Uri.key().isPresent() || s3Uri.key().get().isEmpty()) {
            throw new IllegalArgumentException("Invalid S3 URI - missing key: ${uri}")
        }

        return s3Uri
    }

    static boolean isDirectory(S3Uri s3Uri) {
        return s3Uri.key().isPresent() && s3Uri.key().get().endsWith("/")
    }
}
