package org.eqasim.server.rewrite.api;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.javalin.http.ContentType;
import io.javalin.http.Context;

public abstract class AbstractEndpoint {
	private final ObjectMapper objectMapper = new ObjectMapper();

	protected <T> T readRequest(Context ctx, Class<T> requestType)
			throws JsonMappingException, JsonProcessingException {
		return objectMapper.readValue(ctx.body(), requestType);
	}

	protected <T> List<T> readRequests(Context ctx, Class<T> requestType)
			throws JsonMappingException, JsonProcessingException {
		return objectMapper.readValue(ctx.body(), new TypeReference<List<T>>() {
		});
	}

	protected <T> void writeResponse(Context ctx, T response) throws JsonProcessingException {
		ctx.contentType(ContentType.JSON);
		ctx.result(objectMapper.writeValueAsString(response));
	}
}
