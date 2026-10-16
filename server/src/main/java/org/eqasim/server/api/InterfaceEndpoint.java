package org.eqasim.server.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import io.javalin.http.ContentType;
import io.javalin.http.Context;

public class InterfaceEndpoint {
	private final InterfaceMetadata metadata;
	private final String document;

	public InterfaceEndpoint(InterfaceMetadata metadata) {
		this.metadata = Objects.requireNonNull(metadata);
		try (var stream = InterfaceEndpoint.class.getResourceAsStream("/interface.html")) {
			if (stream == null) {
				throw new IllegalStateException("Missing interface.html classpath resource");
			}
			document = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot load interface.html classpath resource", e);
		}
	}

	public void get(Context ctx) {
		ctx.contentType(ContentType.HTML);
		ctx.result(document);
	}

	public void getMetadata(Context ctx) {
		ctx.json(metadata);
	}
}
