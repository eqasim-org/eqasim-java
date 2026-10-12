package org.eqasim.server.api;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eqasim.server.ServiceBuilder.Services;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.javalin.http.Context;

public class ServicesEndpoint extends AbstractEndpoint {
    final Services services;

    public ServicesEndpoint(Services services) {
        this.services = services;
    }

    public void get(Context ctx) throws JsonProcessingException, InterruptedException, ExecutionException {
        Response response = new Response();

        if (services.roadRouterService() != null) {
            response.services.add("router/road");
        }

        if (services.roadIsochroneService() != null) {
            response.services.add("isochrone/road");
        }

        if (services.transitRouterService() != null) {
            response.services.add("router/transit");
        }

        if (services.transitIsochroneService() != null) {
            response.services.add("isochrone/transit");
        }

        writeResponse(ctx, response);
    }

    static public class Response {
        public List<String> services = new LinkedList<>();
    }
}
