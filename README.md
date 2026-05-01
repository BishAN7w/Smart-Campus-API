# Smart Campus Sensor & Room Management API

**Module:** 5COSC022W – Client-Server Architectures  
**University:** University of Westminster – School of Computer Science and Engineering  
**Module Leader:** Hamed Hamzeh  
**Stack:** JAX-RS (Jersey 2.41) + Grizzly2 Embedded HTTP Server + Java 11

---

## API Overview

A RESTful web service for managing campus rooms and the IoT sensors deployed within them. Built with JAX-RS (Jersey) running on an embedded Grizzly2 HTTP server — no external servlet container required.

**Base URL:** `http://localhost:8080/api/v1/`

| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1 | Discovery endpoint |
| GET | /api/v1/rooms | List all rooms |
| POST | /api/v1/rooms | Create a room |
| GET | /api/v1/rooms/{id} | Get a room |
| DELETE | /api/v1/rooms/{id} | Delete a room |
| GET | /api/v1/sensors | List all sensors |
| POST | /api/v1/sensors | Register a sensor |
| GET | /api/v1/sensors/{id} | Get a sensor |
| DELETE | /api/v1/sensors/{id} | Remove a sensor |
| GET | /api/v1/sensors/{id}/readings | Get reading history |
| POST | /api/v1/sensors/{id}/readings | Add a reading |

---

## Project Structure

```
smart-campus-api/
├── pom.xml
└── src/main/java/com/smartcampus/
    ├── Main.java
    ├── SmartCampusApplication.java
    ├── model/
    │   ├── Room.java
    │   ├── Sensor.java
    │   ├── SensorReading.java
    │   └── ErrorResponse.java
    ├── store/
    │   └── DataStore.java
    ├── resource/
    │   ├── DiscoveryResource.java
    │   ├── RoomResource.java
    │   ├── SensorResource.java
    │   └── SensorReadingResource.java
    ├── exception/
    │   ├── RoomNotEmptyException.java
    │   ├── LinkedResourceNotFoundException.java
    │   ├── SensorUnavailableException.java
    │   └── mapper/
    │       ├── RoomNotEmptyExceptionMapper.java
    │       ├── LinkedResourceNotFoundExceptionMapper.java
    │       ├── SensorUnavailableExceptionMapper.java
    │       └── GlobalExceptionMapper.java
    └── filter/
        └── LoggingFilter.java
```

---

## Build and Run

**Requirements:** Java 11+, Maven 3.6+

**1. Build**
```bash
mvn clean package
```

**2. Run**
```bash
java -jar target/smart-campus-api-1.0-SNAPSHOT.jar
```

The server starts on `http://localhost:8080/api/v1/`.

**3. Stop**  
Press `Ctrl+C`.

---

## Sample curl Commands

### Discovery
```bash
curl http://localhost:8080/api/v1/
```

### Create a room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":50}'
```

### List all rooms
```bash
curl http://localhost:8080/api/v1/rooms
```

### Get a specific room
```bash
curl http://localhost:8080/api/v1/rooms/LIB-301
```

### Register a sensor
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-001","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"LIB-301"}'
```

### List all sensors
```bash
curl http://localhost:8080/api/v1/sensors
```

### Filter sensors by type
```bash
curl "http://localhost:8080/api/v1/sensors?type=Temperature"
```

### Post a reading
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":22.5}'
```

### Get reading history
```bash
curl http://localhost:8080/api/v1/sensors/TEMP-001/readings
```

### Try deleting a room that has sensors (expects 409)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

### Try registering a sensor with an invalid roomId (expects 422)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-001","type":"CO2","status":"ACTIVE","currentValue":0.0,"roomId":"FAKE-ROOM"}'
```

### Try posting a reading to a MAINTENANCE sensor (expects 403)
```bash
# First create a room and a sensor in MAINTENANCE status
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LAB-101","name":"Computer Lab","capacity":30}'

curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"OCC-001","type":"Occupancy","status":"MAINTENANCE","currentValue":0.0,"roomId":"LAB-101"}'

# Now try to post a reading (should return 403)
curl -X POST http://localhost:8080/api/v1/sensors/OCC-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":10.0}'
```

### Delete an empty room
```bash
# Remove the sensor first
curl -X DELETE http://localhost:8080/api/v1/sensors/TEMP-001

# Now delete the room
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

---

## Report – Question Answers

### Part 1.1 – JAX-RS Resource Lifecycle

By default, JAX-RS creates a new instance of each resource class for every incoming HTTP request. The instance is discarded after the response is sent. This makes resource classes thread-safe by default because no state is shared between requests through instance fields.

The consequence for in-memory data is that anything stored as a resource instance field would be lost the moment the request ends. To work around this, all shared data in this project lives in `DataStore`, a singleton that persists for the full lifetime of the JVM. `DataStore` uses `ConcurrentHashMap` instead of `HashMap` so that concurrent requests accessing the same collections do not cause race conditions or data corruption. A plain `HashMap` is not thread-safe — two simultaneous writes can corrupt the internal structure. `ConcurrentHashMap` uses segment-level locking, allowing safe concurrent reads and writes without blocking the entire map.

---

### Part 1.2 – HATEOAS

HATEOAS (Hypermedia as the Engine of Application State) means that API responses include links to related resources and available actions, rather than just data. Clients can navigate the API by following those links rather than by hard-coding URLs.

In this API, every response includes a `_links` object. The discovery endpoint at `GET /api/v1` returns a full navigation map of available resource collections. A sensor response includes links to its readings endpoint and the sensors collection.

The main benefit for client developers is reduced coupling. If paths change, clients that follow links rather than using hard-coded URLs do not need to be updated. A new developer can also start at `GET /api/v1` and navigate the entire API without any external documentation, since each response tells them what they can do next.

---

### Part 2.1 – Returning IDs vs Full Objects

Returning only IDs in a collection response keeps payloads small, but forces the client to make a separate request for each ID to get the actual data. For a list of 50 rooms, that means 51 requests instead of 1. This is the N+1 problem and is generally worse for performance than sending slightly larger payloads.

Returning full objects means one request gets everything the client typically needs. For dashboards and automated systems, this is almost always the right choice. The bandwidth cost is manageable, especially with HTTP compression, and the latency reduction from fewer round-trips is significant.

This API returns full objects in collection responses. For very large datasets, pagination (`?page=1&size=20`) would be added to keep individual response sizes reasonable.

---

### Part 2.2 – Is DELETE Idempotent?

Yes, DELETE is idempotent in this implementation. Idempotency means that making the same request multiple times produces the same server state as making it once.

The first DELETE on an existing empty room removes it and returns 204 No Content. Any subsequent DELETE for the same ID finds nothing and returns 404 Not Found. In both cases the end state is the same — the room does not exist. The status codes differ, but the resource state is identical, which satisfies the HTTP definition of idempotency.

A client handling room deletion should treat both 204 and 404 as successful outcomes (the room is gone) and only treat 409 as an actionable error.

---

### Part 3.1 – @Consumes and Media Type Mismatches

`@Consumes(MediaType.APPLICATION_JSON)` tells JAX-RS that a resource method only handles requests with `Content-Type: application/json`. If a client sends the same request with `Content-Type: text/plain` or `application/xml`, JAX-RS rejects the request before the method is ever called and returns HTTP 415 Unsupported Media Type. No application code runs.

This is handled entirely by JAX-RS's content negotiation layer, which is one of the reasons using the annotation is cleaner than manually checking the content type inside the method. The business logic never has to deal with unexpected payload formats.

---

### Part 3.2 – Query Parameter vs Path Segment for Filtering

`GET /api/v1/sensors?type=CO2` uses a query parameter for filtering, which is correct because the resource being addressed is the sensors collection. The `type` filter is not a resource — it is a constraint on retrieval.

A path segment like `/sensors/type/CO2` implies that `type/CO2` is itself an addressable resource, which is semantically wrong. It also cannot represent "no filter" — you would need a completely separate URL to list all sensors. Multiple filters (e.g., by type and status) become awkward to represent as path segments, whereas query parameters compose naturally: `?type=CO2&status=ACTIVE`.

The general rule is that paths identify resources and query parameters modify how those resources are retrieved.

---

### Part 4.1 – Sub-Resource Locator Pattern

The sub-resource locator pattern lets a parent resource delegate handling of a nested path to a dedicated class. In this project, `SensorResource` has a method annotated with only `@Path("{sensorId}/readings")` and no HTTP verb. JAX-RS calls that method to get a `SensorReadingResource` instance, then dispatches the actual HTTP method (GET or POST) to that instance.

The main benefit is separation of concerns. `SensorResource` handles sensor CRUD. `SensorReadingResource` handles reading history. Each class is smaller, easier to read, and easier to test in isolation. If the readings logic needs to change, only `SensorReadingResource` needs to be touched.

Compared to defining every nested path in one large controller, this approach also scales better. Adding `/sensors/{id}/alerts` later would just mean adding another locator method and a new class, with no changes to the existing sensor or reading logic.

---

### Part 5.2 – HTTP 422 vs 404 for Missing Referenced Resources

404 Not Found means the URL of the request could not be resolved. It signals that the target of the HTTP request does not exist.

422 Unprocessable Entity means the URL was valid and the request body was syntactically correct JSON, but the content of the payload was logically invalid.

When a client posts a sensor with a `roomId` that does not exist, the URL `/api/v1/sensors` resolves correctly. The JSON is valid. The problem is inside the payload — the `roomId` field refers to something that does not exist. Returning 404 here would be misleading because it implies the endpoint itself was not found. Returning 422 correctly tells the client that the request was received and understood, but the data it contained was invalid. The client knows to fix the payload, not the URL.

---

### Part 5.4 – Risks of Exposing Stack Traces

Stack traces give an attacker detailed information about the internals of the application:

**Technology fingerprinting:** Package names and class names reveal which framework and version is running (e.g. `org.glassfish.jersey 2.41`). An attacker can look up known CVEs for that exact version.

**Internal structure:** Stack frames show the full call hierarchy — package names, class names, method names, and line numbers. This helps an attacker understand how the code is organised and craft targeted requests.

**Business logic hints:** Method names often reflect what the code does (e.g. `validateSensorReading`). An attacker can infer what checks exist and try to work around them.

**Dependency disclosure:** Third-party library frames reveal which ORM, JSON parser, or other libraries are in use, narrowing the attack surface further.

The `GlobalExceptionMapper` in this project prevents all of this. Any unhandled `Throwable` is caught, logged on the server with full detail for engineers to diagnose, and the client only receives a generic 500 response with no internal information.

---

### Part 5.5 – Why Use Filters for Logging Instead of Manual Logger Calls

Placing logging inside a JAX-RS filter rather than adding `Logger.info()` calls to every resource method has several practical advantages.

A filter is registered once and runs automatically for every single request and response, regardless of which endpoint is hit. This means a new resource class added later gets logging for free without any extra work. Manual logging inside each method would require remembering to add it every time, and forgetting even once creates gaps in the log history.

It also keeps the resource methods clean. A method like `createRoom` should only contain room creation logic. When logging is mixed in, the method is doing two things at once, which makes it harder to read and maintain.

The format stays consistent too. With a single filter controlling the log output, every entry looks the same. Manual per-method logging tends to drift — different developers write messages differently, use different log levels, or include different details.

Finally, if the logging format needs to change, only `LoggingFilter.java` needs to be edited. With manual logging spread across 10 resource methods, the same change would need to be made in 10 places.
