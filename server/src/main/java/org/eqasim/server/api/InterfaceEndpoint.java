package org.eqasim.server.api;

import io.javalin.http.ContentType;
import io.javalin.http.Context;

public class InterfaceEndpoint {
    public void get(Context ctx) {
        ctx.contentType(ContentType.HTML);
		ctx.result("THIS IS A PLACEHOLDER");
    }
}
