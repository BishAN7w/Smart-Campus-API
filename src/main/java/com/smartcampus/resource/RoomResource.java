package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.store.DataStore;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles /api/v1/rooms
 *
 * GET    /           - list all rooms
 * POST   /           - create a new room
 * GET    /{roomId}   - get a specific room
 * DELETE /{roomId}   - delete a room (blocked if sensors are still assigned)
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final DataStore dataStore = DataStore.getInstance();

    @GET
    public Response getAllRooms() {
        return Response.ok(dataStore.getAllRooms()).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRoom(Room room) {
        if (room == null || room.getId() == null || room.getId().isBlank()) {
            return badRequest("Room 'id' is required.");
        }
        if (room.getName() == null || room.getName().isBlank()) {
            return badRequest("Room 'name' is required.");
        }
        if (room.getCapacity() <= 0) {
            return badRequest("Room 'capacity' must be a positive integer.");
        }
        if (dataStore.roomExists(room.getId())) {
            return conflict("A room with id '" + room.getId() + "' already exists.");
        }

        dataStore.addRoom(room);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Room created successfully.");
        response.put("room", room);
        response.put("_links", roomLinks(room.getId()));
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    @Path("{roomId}")
    public Response getRoomById(@PathParam("roomId") String roomId) {
        Room room = dataStore.getRoomById(roomId);
        if (room == null) {
            return notFound("Room with id '" + roomId + "' does not exist.");
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("room", room);
        response.put("_links", roomLinks(roomId));
        return Response.ok(response).build();
    }

    /**
     * DELETE /{roomId}
     *
     * A room cannot be deleted while it has sensors assigned to it.
     * Attempting to do so throws RoomNotEmptyException, mapped to 409 Conflict.
     *
     * Idempotency: the first call on an existing empty room returns 204.
     * Subsequent calls for the same ID return 404. The end state is identical
     * in both cases (the room does not exist), satisfying idempotency from a
     * resource-state perspective.
     */
    @DELETE
    @Path("{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = dataStore.getRoomById(roomId);
        if (room == null) {
            return notFound("Room with id '" + roomId + "' does not exist.");
        }
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(roomId);
        }
        dataStore.deleteRoom(roomId);
        return Response.noContent().build();
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

    private Map<String, String> roomLinks(String roomId) {
        Map<String, String> links = new LinkedHashMap<>();
        links.put("self",    "/api/v1/rooms/" + roomId);
        links.put("all",     "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        return links;
    }
}
