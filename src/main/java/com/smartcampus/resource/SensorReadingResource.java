package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sub-resource for /api/v1/sensors/{sensorId}/readings
 *
 * Instantiated by SensorResource's sub-resource locator method.
 * Receives the sensorId context through its constructor.
 *
 * GET  / - retrieve reading history for the sensor
 * POST / - append a new reading (blocked if sensor is MAINTENANCE or OFFLINE)
 *
 * A successful POST also updates the parent sensor's currentValue field.
 */
@Produces(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;
    private final DataStore dataStore = DataStore.getInstance();

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    @GET
    public Response getReadings() {
        Sensor sensor = dataStore.getSensorById(sensorId);
        if (sensor == null) {
            return notFound("Sensor with id '" + sensorId + "' does not exist.");
        }

        List<SensorReading> history = dataStore.getReadingsForSensor(sensorId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("sensorId", sensorId);
        response.put("sensorType", sensor.getType());
        response.put("currentValue", sensor.getCurrentValue());
        response.put("readingCount", history.size());
        response.put("readings", history);
        response.put("_links", readingLinks());
        return Response.ok(response).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading reading) {
        Sensor sensor = dataStore.getSensorById(sensorId);
        if (sensor == null) {
            return notFound("Sensor with id '" + sensorId + "' does not exist.");
        }
        if (Sensor.STATUS_MAINTENANCE.equalsIgnoreCase(sensor.getStatus())
                || Sensor.STATUS_OFFLINE.equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(sensorId, sensor.getStatus());
        }
        if (reading == null) {
            return badRequest("Reading payload is required.");
        }
        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(UUID.randomUUID().toString());
        }
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        dataStore.addReading(sensorId, reading);
        sensor.setCurrentValue(reading.getValue());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Reading recorded successfully.");
        response.put("sensorId", sensorId);
        response.put("updatedCurrentValue", sensor.getCurrentValue());
        response.put("reading", reading);
        response.put("_links", readingLinks());
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    private Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorBody(400, "Bad Request", message)).build();
    }

    private Response notFound(String message) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(errorBody(404, "Not Found", message)).build();
    }

    private Map<String, Object> errorBody(int status, String error, String message) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("status", status);
        err.put("error", error);
        err.put("message", message);
        err.put("timestamp", System.currentTimeMillis());
        return err;
    }

    private Map<String, String> readingLinks() {
        Map<String, String> links = new LinkedHashMap<>();
        links.put("self",         "/api/v1/sensors/" + sensorId + "/readings");
        links.put("parentSensor", "/api/v1/sensors/" + sensorId);
        links.put("allSensors",   "/api/v1/sensors");
        return links;
    }
}
