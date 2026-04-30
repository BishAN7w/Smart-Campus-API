package com.smartcampus.exception;

/**
 * Thrown when an attempt is made to delete a Room that still has active Sensors assigned to it.
 * Mapped to HTTP 409 Conflict by RoomNotEmptyExceptionMapper.
 */
public class RoomNotEmptyException extends RuntimeException {

    private final String roomId;

    public RoomNotEmptyException(String roomId) {
        super("Room '" + roomId + "' cannot be deleted because it still has sensors assigned to it. "
                + "Please remove or reassign all sensors before deleting this room.");
        this.roomId = roomId;
    }

    public String getRoomId() {
        return roomId;
    }
}
