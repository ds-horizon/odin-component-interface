package com.dream11.lock

import com.fasterxml.jackson.annotation.JsonProperty

record LockConfig(@JsonProperty("provider") String provider, @JsonProperty("config") Map<String, Object> config) {}
