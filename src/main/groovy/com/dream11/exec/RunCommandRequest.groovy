package com.dream11.exec

class RunCommandRequest {
    private String command
    private String workingDir
    private boolean silent

    RunCommandRequest(String command, String workingDir) {
        this.command = command
        this.workingDir = workingDir
    }

    RunCommandRequest(String command, String workingDir, boolean silent) {
        this.command = command
        this.workingDir = workingDir
        this.silent = silent
    }

    String getCommand() {
        return command
    }

    void setCommand(String command) {
        this.command = command
    }

    String getWorkingDir() {
        return workingDir
    }

    boolean isSilent() {
        return silent
    }
}
