package com.dream11.spec


import com.dream11.ExecutionContext
import com.dream11.exec.CommandResponse

interface Spec {
    void validate(ExecutionContext context)

    List<CommandResponse> execute(ExecutionContext context)

}
