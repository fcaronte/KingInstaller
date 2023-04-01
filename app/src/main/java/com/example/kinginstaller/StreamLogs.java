package com.example.kinginstaller;

public class StreamLogs {
    private String inputStreamLog;
    private String errorStreamLog;
    private String outputStreamLog;

    public StreamLogs() {}

    public String getInputStreamLog() {
        if (inputStreamLog == null) { return ""; }
        return inputStreamLog.trim();
    }

    public String getErrorStreamLog() {
        if (errorStreamLog == null) { return ""; }
        return errorStreamLog.trim();
    }

    public String getOutputStreamLog() {
        if (outputStreamLog == null) { return ""; }
        return outputStreamLog.trim();
    }

    public void setInputStreamLog(String inputStreamLog) { this.inputStreamLog = inputStreamLog; }

    public void setErrorStreamLog(String errorStreamLog) { this.errorStreamLog = errorStreamLog; }

    public void setOutputStreamLog(String outputStreamLog) {
        this.outputStreamLog = outputStreamLog;
    }

    public String getInputStreamLogWithLabel() {
        return "\tInputStream:\n\t\t" +
                getInputStreamLog().replaceAll("\n", "\n\t\t");
    }

    public String getErrorStreamLogWithLabel() {
        return "\tErrorStream:\n\t\t" +
                getErrorStreamLog().replaceAll("\n", "\n\t\t");
    }

    public String getOutputStreamLogWithLabel() {
        return "\tOutputStream:\n\t\t" +
                getOutputStreamLog().replaceAll("\n", "\n\t\t");
    }

    public String getStreamLogsWithLabels() {
        String result = "\n" + getOutputStreamLogWithLabel();
        if (!getInputStreamLog().isEmpty()) { result += "\n" + getInputStreamLogWithLabel(); }
        if (!getErrorStreamLog().isEmpty()) { result += "\n" + getErrorStreamLogWithLabel(); }
        return result;
    }
}
