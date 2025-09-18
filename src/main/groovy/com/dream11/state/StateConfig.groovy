package com.dream11.state

import com.fasterxml.jackson.annotation.JsonProperty

class StateConfig {
    String provider
    String uri
    Credentials credentials

    StateConfig(@JsonProperty("provider") String provider,
                @JsonProperty("uri") String uri,
                @JsonProperty("credentials") Credentials credentials) {
        this.provider = provider
        this.uri = uri
        this.credentials = credentials
    }

    String getProvider() {
        return provider
    }

    String getUri() {
        return uri
    }

    Credentials getCredentials() {
        return credentials
    }

}
