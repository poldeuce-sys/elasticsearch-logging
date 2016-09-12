package com.polydeucesys.eslogging.logback;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.google.gson.Gson;
import com.polydeucesys.eslogging.core.*;
import com.polydeucesys.eslogging.core.gson.SimpleGsonLogSerializer;
import com.polydeucesys.eslogging.core.jest.*;
import io.searchbox.client.JestResult;
import io.searchbox.core.Bulk;
import io.searchbox.core.Index;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
/**
 * Copyright (c) 2016 Polydeuce-Sys Ltd
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 **/
/**
 * Provides the Appender implementation for logback. The appender wraps the
 * {@link LogAppenderModule} functionality and provides a means of setting the configuration
 * of the module.
 *
 * @author Kevin McLellan
 * @version 1.0
 */
public class BaseElasticsearchJestAppender extends AppenderBase<ILoggingEvent> {

    private static final String FAILED_STOP_MSG = "Exception closing BaseElasticsearchJestAppender appender module";
    private static final String FAILED_START_MSG = "Exception starting BaseElasticsearchJestAppender appender module";

    // Settable properties
    private String connectionString = "";
    private String logIndexPrefix = "";
    private String logIndexDateFormat = Constants.ELASTICSEARCH_INDEX_DATE_STAMP_FORMAT;
    private String logDocType = "";
    private final Map<String, String> logContextPropertiesMap = new HashMap<String,String>();

    private int queueDepth = JestConstants.DEFAULT_QUEUE_DEPTH;
    private long maxSubmissionInterval = JestConstants.DEFAULT_MAX_SUBMISSION_INTERVAL_MILLISECONDS;
    private boolean isOutputLoggerErrorsToStdErr = true;
    // Internals
    private LogAppenderModule<ILoggingEvent, ILoggingEvent, Index, Bulk, JestResult> loggingModule;
    private final Object moduleBuildLock = new Object();
    // for logback lifecycle management purposes
    private final ILoggingEventTypeAdapter iLoggingEventTypeAdapter = new ILoggingEventTypeAdapter();

    private final LogbackStatusManagerErrorHandler errorHandler = new LogbackStatusManagerErrorHandler();

    public void setConnectionString(String connectionString){
        this.connectionString = connectionString;
    }

    public String getConnectionString(){
        return connectionString;
    }

    public int getQueueDepth() {
        return queueDepth;
    }

    public void setQueueDepth(int queueDepth) {
        this.queueDepth = queueDepth;
    }

    public long getMaxSubmissionInterval() {
        return maxSubmissionInterval;
    }

    public void setMaxSubmissionInterval(long maxSubmissionInterval) {
        this.maxSubmissionInterval = maxSubmissionInterval;
    }

    public boolean isOutputLoggerErrorsToStdErr() {
        return isOutputLoggerErrorsToStdErr;
    }

    public void setOutputLoggerErrorsToStdErr(boolean isOutputLoggerErrorsToStdErr) {
        this.isOutputLoggerErrorsToStdErr = isOutputLoggerErrorsToStdErr;
    }

    public String getLogIndexPrefix() {
        return logIndexPrefix;
    }

    public void setLogIndexPrefix(String logIndexPrefix) {
        this.logIndexPrefix = logIndexPrefix;
    }

    public String getLogIndexDateFormat() {
        return logIndexDateFormat;
    }

    public void setLogIndexDateFormat(String logIndexDateFormat) {
        this.logIndexDateFormat = logIndexDateFormat;
    }

    public String getLogDocType() {
        return logDocType;
    }

    public void setLogDocType(String logDocType) {
        this.logDocType = logDocType;
    }

    public String getLogContextPropertiesJson(){
        Gson gson = new Gson();
        return gson.toJson(logContextPropertiesMap);
    }

    @SuppressWarnings("unchecked")
    public void setLogContextPropertiesJson(String propertiesJson ){
        Gson gson = new Gson();
        logContextPropertiesMap.putAll((Map<String,String>)gson.fromJson(propertiesJson,
                logContextPropertiesMap.getClass()));
    }

    private LogAppenderErrorHandler setupErrorHandler( ){
        LogAppenderErrorHandler esJestErrorHandler = isOutputLoggerErrorsToStdErr ?
                new JestLogSubmissionStrategy.LogSubmissionStdErrErrorHandler() :
                new LogbackStatusManagerErrorHandler();
        return esJestErrorHandler;
    }

    public LogAppenderErrorHandler getErrorHandler() {
        return this.errorHandler;
    }


    @Override
    public void start(){
        synchronized (moduleBuildLock){
            super.start();
            LogTransform<ILoggingEvent, ILoggingEvent> logTransform = (LogTransform<ILoggingEvent, ILoggingEvent>) new HostAndCwdLogTransform(logContextPropertiesMap);

            LogMapper<ILoggingEvent> dateStampedLogMapper = new SimpleDateStampedLogMapper<ILoggingEvent>(getLogIndexPrefix(),
                    getLogIndexDateFormat(),
                    getLogDocType());
            iLoggingEventTypeAdapter.start();
            SimpleGsonLogSerializer<ILoggingEvent> wrappedJsonStringSerializer = new SimpleGsonLogSerializer<ILoggingEvent>();
            wrappedJsonStringSerializer.setTypeAdapter(ILoggingEvent.class, iLoggingEventTypeAdapter);
            wrappedJsonStringSerializer.setTypeAdapter(LoggingEvent.class, iLoggingEventTypeAdapter);
            JestIndexStringSerializerWrapper<ILoggingEvent> logSerializer = new JestIndexStringSerializerWrapper<ILoggingEvent>();
            logSerializer.setWrappedJsonStringSerializer(wrappedJsonStringSerializer);
            logSerializer.setIndexMapper(dateStampedLogMapper);
            Connection<Bulk, JestResult> connection = new JestHttpConnection();
            connection.setConnectionString(connectionString);
            LogSubmissionQueueingStrategy<Index, Bulk, JestResult> logSubmissionStrategy = new JestLogSubmissionStrategy();
            logSubmissionStrategy.setConnection(connection);
            logSubmissionStrategy.setQueueDepth(queueDepth);
            logSubmissionStrategy.setMaxSubmissionIntervalMillisec(maxSubmissionInterval);
            loggingModule = new LogAppenderModule<ILoggingEvent, ILoggingEvent, Index, Bulk, JestResult>(	logTransform,
                    logSerializer,
                    logSubmissionStrategy,
                    errorHandler);
            try {
                loggingModule.start();
            } catch (LogSubmissionException e) {
                errorHandler.error(FAILED_START_MSG, e);
            }
        }
    }

    @Override
    protected void append(ILoggingEvent e) {
        loggingModule.append(e);
    }
}