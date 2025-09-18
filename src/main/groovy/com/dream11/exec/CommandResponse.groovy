package com.dream11.exec

class CommandResponse {
    private String command
    private String stdOut
    private String stdErr
    private boolean exportable
    private int exitCode

    CommandResponse(String command, String stdOut, String stdErr, boolean exportable = false, int exitCode) {
        this.command = command
        this.stdOut = stdOut
        this.stdErr = stdErr
        this.exportable = exportable
        this.exitCode = exitCode
    }

    String getStdOut() {
        return stdOut
    }

    String getStdErr() {
        return stdErr
    }

    boolean hasError() {
        return exitCode != 0
    }

    String getCommand() {
        return command
    }

    void setCommand(String command) {
        this.command = command
    }

    boolean getExportable() {
        return exportable
    }

    void setExportable(boolean exportable) {
        this.exportable = exportable
    }

    void setStdErr(String stdErr) {
        this.stdErr = stdErr
    }
}
