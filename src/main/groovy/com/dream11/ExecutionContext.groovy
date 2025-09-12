package com.dream11

import com.fasterxml.jackson.annotation.JsonIgnore

import static com.dream11.Constants.DSL_NAME

class ExecutionContext {

    /**
     * Represents the directory from where the component script will be executed.
     * */
    @JsonIgnore
    private final String metaWorkingDir

    /**
     * Represents working directory of the component. Flavour stages (deploy/undeploy) will be running from this
     * directory.
     *
     * Its required because component DSL provided by component author will have template variables.
     * These variables will have to replaced before actually invoking deploy/undeploy/healthcheck.
     *
     * Hence, {@link this.metaWorkingDir} represents component "as is" and workingDir represents component with
     * template variables replaced with their actual values.
     * */
    private final String workingDir

    private final DslMetadata metadata

    ExecutionContext(DslMetadata metadata, String metaWorkingDir,
                     String workingDir) {
        this.workingDir = workingDir
        this.metaWorkingDir = metaWorkingDir
        this.metadata = metadata
    }

    DslMetadata getMetadata() {
        return metadata
    }

    String getWorkingDir() {
        return workingDir
    }

    String getMetaWorkingDir() {
        return metaWorkingDir
    }

    File getDSLFileInMetaDir() {
        return new File(getMetaWorkingDir() + File.separator + DSL_NAME)
    }

    File getDSLFileInWorkingDir() {
        return new File(getWorkingDir() + File.separator + DSL_NAME)
    }
}
