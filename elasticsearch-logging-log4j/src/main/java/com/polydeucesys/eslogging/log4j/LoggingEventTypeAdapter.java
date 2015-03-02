package com.polydeucesys.eslogging.log4j;

import java.io.IOException;
import java.util.Set;

import org.apache.log4j.spi.LoggingEvent;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class LoggingEventTypeAdapter extends TypeAdapter<LoggingEvent> {
	private static final String WRITE_ONLY_ADAPTER = "Only write operations should be used in the LogAppender";

	private static final String TIMESTAMP_KEY = "@timestamp";
	private static final String START_TIME_KEY = "startTime";
	private static final String LOGGER_NAME_KEY = "logger";
	private static final String THREAD_NAME_KEY = "threadName";
	private static final String LOCATION_KEY = "locationInfo";
	
	private static final String LEVEL_KEY = "level";
	private static final String NDC_KEY = "ndc";
	private static final String MESSAGE_KEY = "message";
	private static final String THROWABLE_KEY = "throwable";
	private static final String PROPERTIES_KEY = "properties";
	
	
	
	@Override
	public LoggingEvent read(JsonReader arg0) throws IOException {
		throw new IllegalStateException(WRITE_ONLY_ADAPTER);
	}

	@Override
	public void write(JsonWriter writer, LoggingEvent log) throws IOException {
		if (log == null) {
			writer.nullValue();
			return;
	    }
		writer.beginObject();
		writer.name(LOGGER_NAME_KEY).value(log.getLoggerName());
		writer.name(TIMESTAMP_KEY).value(log.getTimeStamp());
		writer.name(START_TIME_KEY).value(LoggingEvent.getStartTime());
		writer.name(THREAD_NAME_KEY).value(log.getThreadName());
		writer.name(LOCATION_KEY).value(log.getLocationInformation().fullInfo);
		writer.name(LEVEL_KEY).value(log.getLevel().toString());
		writer.name(NDC_KEY).value(log.getNDC());
		writer.name(MESSAGE_KEY).value(log.getRenderedMessage());
		if(log.getThrowableInformation() != null){
			writer.name(THROWABLE_KEY);
			writer.beginArray();
			for(String t : log.getThrowableStrRep()){
				writer.value(t);
			}
			writer.endArray();
		}else{
			writer.name(THROWABLE_KEY).nullValue();
		}
		@SuppressWarnings("unchecked")
		Set<String> propKeys = log.getPropertyKeySet();

		if(propKeys.size() > 0){
			writer.name(PROPERTIES_KEY);
			writer.beginObject();
			for(String key : propKeys){
				writer.name(key).value(log.getProperty(key));
			}
			writer.endObject();
		}else{
			writer.name(PROPERTIES_KEY).nullValue();
		}
		writer.endObject();
	}

}
