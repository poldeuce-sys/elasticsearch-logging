package com.polydeucesys.eslogging.core;

/**
 * Defines the base interface for a strategy for log submission. This requires a connection, an error handler and
 * a call point for submission of a log object from the logging framework.
 * @author Kevin McLellan
 * @version 1.0
 *
 * @param <L> 
 * 			Type of log object submitted by the logging framework
 * @param <D>
 * 			Type of document expected by the log storage
 * @param <R>
 * 			Type of response given by storage on successfulcsubmission
 */
public interface LogSubmissionStrategy<L,D,R> {
	void setConnection(final Connection<D, R> connection);
	Connection<D, R> getConnection();
	void setErrorHandler( LogAppenderErrorHandler errorHandler);
	LogAppenderErrorHandler getErrorHandler();
	void submit( final L log ) throws LogSubmissionException;
}
