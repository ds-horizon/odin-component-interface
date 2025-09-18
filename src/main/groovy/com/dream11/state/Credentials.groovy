package com.dream11.state

import com.fasterxml.jackson.annotation.JsonProperty


record Credentials(@JsonProperty("username") String username,@JsonProperty("password") String password) {}
