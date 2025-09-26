package com.dream11.lock

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size


class RedisLockClientConfig implements LockClientConfig {
    @NotBlank(message = "Redis key cannot be blank")
    @Size(min = 1, max = 256, message = "Redis key must be between 1 and 256 characters")
    @Pattern(regexp = "^[a-zA-Z0-9:_.-]+\$", message = "Redis key can only contain alphanumeric characters, colons, underscores, dots, and hyphens")
    private String key

    @NotBlank(message = "Redis host cannot be blank")
    @Size(min = 1, max = 255, message = "Host must be between 1 and 255 characters")
    private String host

    @NotNull(message = "Redis port cannot be null")
    @Min(value = 1, message = "Redis port must be at least 1")
    @Max(value = 65535, message = "Redis port cannot exceed 65535")
    private Integer port = 6379

    String getKey() {
        return key
    }

    String getHost() {
        return host
    }

    Integer getPort() {
        return port
    }
}
