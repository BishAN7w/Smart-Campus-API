package com.smartcampus.exception;

/**
 * Thrown when a resource references another resource that does not exist.
 * Primary use case: a POST /sensors request that specifies a roomId which
 * does not correspond to any known Room in the system.
 *
 * Mapped to HTTP 422 Unprocessable Entity by LinkedResourceNotFoundExceptionMapper.
 */
public class LinkedResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final String resourceId;

    public LinkedResourceNotFoundException(String resourceType, String resourceId) {
        super("Linked resource not found: " + resourceType + " with id '" + resourceId
                + "' does not exist. Please ensure the referenced resource exists before creating this entity.");
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }
}
