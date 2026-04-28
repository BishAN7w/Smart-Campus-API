package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GET /api/v1
 *
 * Returns API metadata and a hypermedia link map so clients can discover
 * available resources without relying on external documentation.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("apiName", "Smart Campus Sensor & Room Management API");
        response.put("version", "1.0.0");
        response.put("description", "RESTful API for managing campus rooms and IoT sensors, built with JAX-RS and Grizzly2.");
        response.put("contact", "admin@westminster.ac.uk");
        response.put("moduleLeader", "Hamed Hamzeh");
        response.put("institution", "University of Westminster");

        Map<String, Object> links = new LinkedHashMap<>();
        links.put("self",    buildLink("GET", "/api/v1",         "This discovery document"));
        links.put("rooms",   buildLink("GET", "/api/v1/rooms",   "List all campus rooms"));
        links.put("sensors", buildLink("GET", "/api/v1/sensors", "List all sensors (supports ?type= filter)"));
        response.put("_links", links);

        Map<String, Object> operations = new LinkedHashMap<>();

        Map<String, String> roomOps = new LinkedHashMap<>();
        roomOps.put("GET /api/v1/rooms",         "Retrieve all rooms");
        roomOps.put("POST /api/v1/rooms",         "Create a new room");
        roomOps.put("GET /api/v1/rooms/{id}",     "Retrieve a specific room");
        roomOps.put("DELETE /api/v1/rooms/{id}",  "Delete a room (fails if sensors are assigned)");
        operations.put("rooms", roomOps);

        Map<String, String> sensorOps = new LinkedHashMap<>();
        sensorOps.put("GET /api/v1/sensors",                "List all sensors (optional ?type= filter)");
        sensorOps.put("POST /api/v1/sensors",               "Register a new sensor");
        sensorOps.put("GET /api/v1/sensors/{id}",           "Retrieve a specific sensor");
        sensorOps.put("GET /api/v1/sensors/{id}/readings",  "Retrieve reading history for a sensor");
        sensorOps.put("POST /api/v1/sensors/{id}/readings", "Record a new reading for a sensor");
        operations.put("sensors", sensorOps);

        response.put("operations", operations);

        return Response.ok(response).build();
    }

    private Map<String, String> buildLink(String method, String href, String description) {
        Map<String, String> link = new LinkedHashMap<>();
        link.put("method", method);
        link.put("href", href);
        link.put("description", description);
        return link;
    }
}
