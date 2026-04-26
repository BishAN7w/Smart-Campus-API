package com.smartcampus.store;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton in-memory data store backed by ConcurrentHashMap.
 *
 * JAX-RS creates a new resource class instance per request, so shared state
 * must live outside those instances. This class holds all application data
 * for the entire JVM lifetime. ConcurrentHashMap handles concurrent requests
 * safely without needing explicit synchronisation.
 */
public class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    private DataStore() {}

    public static DataStore getInstance() {
        return INSTANCE;
    }

    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Sensor> sensors = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    // Room operations

    public Collection<Room> getAllRooms() {
        return rooms.values();
    }

    public Room getRoomById(String id) {
        return rooms.get(id);
    }

    public void addRoom(Room room) {
        rooms.put(room.getId(), room);
    }

    public boolean roomExists(String id) {
        return rooms.containsKey(id);
    }

    public void deleteRoom(String id) {
        rooms.remove(id);
    }

    // Sensor operations

    public Collection<Sensor> getAllSensors() {
        return sensors.values();
    }

    public Sensor getSensorById(String id) {
        return sensors.get(id);
    }

    public void addSensor(Sensor sensor) {
        sensors.put(sensor.getId(), sensor);
        readings.putIfAbsent(sensor.getId(), new ArrayList<>());
    }

    public boolean sensorExists(String id) {
        return sensors.containsKey(id);
    }

    public void deleteSensor(String id) {
        sensors.remove(id);
        readings.remove(id);
    }

    // SensorReading operations

    public List<SensorReading> getReadingsForSensor(String sensorId) {
        return readings.getOrDefault(sensorId, new ArrayList<>());
    }

    public void addReading(String sensorId, SensorReading reading) {
        readings.computeIfAbsent(sensorId, k -> new ArrayList<>()).add(reading);
    }
}
