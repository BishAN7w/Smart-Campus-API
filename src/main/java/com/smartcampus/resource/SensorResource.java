package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles /api/v1/sensors
 *
 * GET    /             - list all sensors (optional ?type= filter)
 * POST   /             - register a new sensor
 * GET    /{sensorId}   - get a specific sensor
 * DELETE /{sensorId}   - remove a sensor
 * *      /{sensorId}/readings - delegated to SensorReadingResource
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
public class SensorResource {

    private final DataStore dataStore = DataStore.getInstance();

    /**
     * GET /api/v1/sensors
     *
     * The optional ?type= query parameter filters by sensor type (e.g. ?type=CO2).
     * @QueryParam is used here rather than a path segment like /sensors/type/CO2
     * because query parameters are the HTTP-standard way to express optional filtering
     * on a collection. The path identifies the resource; query parameters refine retrieval.
     * A path segment would imply a distinct sub-resource and cannot represent "no filter".
     */
    @GET
    public Response getAllSensors(@QueryParam("type") String type) {
        Collection<Sensor> all = dataStore.getAllSensors();
        List<Sensor> result;

        if (type != null && !type.isBlank()) {
            result = all.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        } else {
            result = all.stream().collect(Collectors.toList());
        }

        return Response.ok(result).build();
    }

    /**
     * POST /api/v1/sensors
     *
     * @Consumes(APPLICATION_JSON) means JAX-RS only dispatches this method for
     * requests with Content-Type: application/json. Any other content type
     * (text/plain, application/xml, etc.) is rejected by the framework before
     * reaching this method and returns HTTP 415 Unsupported Media Type automatically.
     *
     * The roomId in the request body must reference an existing room. If it does not,
     * LinkedResourceNotFoundException is thrown and mapped to 422.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor) {
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            return badRequest("Sensor 'id' is required.");
        }
        if (sensor.getType() == null || sensor.getType().isBlank()) {
            return badRequest("Sensor 'type' is required.");
        }
        if (sensor.getRoomId() == null || sensor.getRoomId().isBlank()) {
            return badRequest("Sensor 'roomId' is required.");
        }
        if (sensor.getStatus() == null || sensor.getStatus().isBlank()) {
            sensor.setStatus(Sensor.STATUS_ACTIVE);
        }
        if (dataStore.sensorExists(sensor.getId())) {
            return conflict("A sensor with id '" + sensor.getId() + "' already exists.");
        }
        if (!dataStore.roomExists(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException("Room", sensor.getRoomId());
        }

        dataStore.addSensor(sensor);

        Room room = dataStore.getRoomById(sensor.getRoomId());
        if (room != null) {
            room.addSensorId(sensor.getId());
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Sensor registered successfully.");
        response.put("sensor", sensor);
        response.put("_links", sensorLinks(sensor.getId()));
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("{sensorId}")
    public Response getSensorById(@PathParam("sensorId") String sensorId) {
        Sensor sensor = dataStore.getSensorById(sensorId);
        if (sensor == null) {
            return notFound("Sensor with id '" + sensorId + "' does not exist.");
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sensor", sensor);
        response.put("_links", sensorLinks(sensorId));
        return Response.ok(response).build();
    }

    @DELETE
    @Path("{sensorId}")
    public Response deleteSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = dataStore.getSensorById(sensorId);
        if (sensor == null) {
            return notFound("Sensor with id '" + sensorId + "' does not exist.");
        }
        Room room = dataStore.getRoomById(sensor.getRoomId());
        if (room != null) {
            room.removeSensorId(sensorId);
        }
        dataStore.deleteSensor(sensorId);
        return Response.noContent().build();
    }

    /**
     * Sub-resource locator for /api/v1/sensors/{sensorId}/readings
     *
     * This method carries no HTTP verb annotation. JAX-RS calls it to get the
     * sub-resource object, then dispatches the actual HTTP method to that object.
     * This keeps SensorResource focused on sensor CRUD and delegates all reading
     * logic to SensorReadingResource, which can be developed and tested independently.
     */
    @Path("{sensorId}/readings")
    public SensorReadingResource getReadingsSubResource(@PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }

    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorBody(400, "Bad Request", message)).build();
    }

    private Response notFound(String message) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(errorBody(404, "Not Found", message)).build();
    }

    private Response conflict(String message) {
        return Response.status(Response.Status.CONFLICT)
                .entity(errorBody(409, "Conflict", message)).build();
    }

    private Map<String, Object> errorBody(int status, String error, String message) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("status", status);
        err.put("error", error);
        err.put("message", message);
        err.put("timestamp", System.currentTimeMillis());
        return err;
    }

    private Map<String, String> sensorLinks(String sensorId) {
        Map<String, String> links = new LinkedHashMap<>();
        links.put("self",     "/api/v1/sensors/" + sensorId);
        links.put("readings", "/api/v1/sensors/" + sensorId + "/readings");
        links.put("all",      "/api/v1/sensors");
        links.put("rooms",    "/api/v1/rooms");
        return links;
    }
}
