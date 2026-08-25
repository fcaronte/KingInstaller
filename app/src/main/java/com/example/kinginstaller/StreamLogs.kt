package com.example.kinginstaller

class StreamLogs {
    private var inputStreamLog: String? = null
    private var errorStreamLog: String? = null
    private var outputStreamLog: String? = null

    fun getInputStreamLog(): String {
        if (inputStreamLog == null) {
            return ""
        }
        return inputStreamLog!!.trim { it <= ' ' }
    }

    fun getErrorStreamLog(): String {
        if (errorStreamLog == null) {
            return ""
        }
        return errorStreamLog!!.trim { it <= ' ' }
    }

    fun getOutputStreamLog(): String {
        if (outputStreamLog == null) {
            return ""
        }
        return outputStreamLog!!.trim { it <= ' ' }
    }

    fun setInputStreamLog(inputStreamLog: String?) {
        this.inputStreamLog = inputStreamLog
    }

    fun setErrorStreamLog(errorStreamLog: String?) {
        this.errorStreamLog = errorStreamLog
    }

    fun setOutputStreamLog(outputStreamLog: String?) {
        this.outputStreamLog = outputStreamLog
    }

    val inputStreamLogWithLabel: String
        get() = "\tInputStream:\n\t\t" +
                getInputStreamLog().replace("\n".toRegex(), "\n\t\t")

    val errorStreamLogWithLabel: String
        get() = "\tErrorStream:\n\t\t" +
                getErrorStreamLog().replace("\n".toRegex(), "\n\t\t")

    val outputStreamLogWithLabel: String
        get() = "\tOutputStream:\n\t\t" +
                getOutputStreamLog().replace("\n".toRegex(), "\n\t\t")

    val streamLogsWithLabels: String
        get() {
            var result = "\n" + this.outputStreamLogWithLabel
            if (!getInputStreamLog().isEmpty()) {
                result += "\n" + this.inputStreamLogWithLabel
            }
            if (!getErrorStreamLog().isEmpty()) {
                result += "\n" + this.errorStreamLogWithLabel
            }
            return result
        }
}
