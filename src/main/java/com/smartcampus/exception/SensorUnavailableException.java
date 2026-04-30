package com.smartcampus.exception;

/**
 * Thrown when a client attempts to POST a new reading to a Sensor that is currently
 * in MAINTENANCE status and therefore physically unavailable to accept new data.
 *
 * Mapped to HTTP 403 Forbidden by SensorUnavailableExceptionMapper.
 */
public class SensorUnavailableException extends RuntimeException {

    private final String sensorId;
    private final String status;

    public SensorUnavailableException(String sensorId, String status) {
        super("Sensor '" + sensorId + "' is currently in '" + status
                + "' status and cannot accept new readings. "
                + "The sensor must be ACTIVE before readings can be recorded.");
        this.sensorId = sensorId;
        this.status = status;
    }

    public String getSensorId() {
        return sensorId;
    }

    public String getStatus() {
        return status;
    }
}
