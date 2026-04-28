package com.smartcampus;

import com.smartcampus.exception.mapper.GlobalExceptionMapper;
import com.smartcampus.exception.mapper.LinkedResourceNotFoundExceptionMapper;
import com.smartcampus.exception.mapper.RoomNotEmptyExceptionMapper;
import com.smartcampus.exception.mapper.SensorUnavailableExceptionMapper;
import com.smartcampus.filter.LoggingFilter;
import com.smartcampus.resource.DiscoveryResource;
import com.smartcampus.resource.RoomResource;
import com.smartcampus.resource.SensorResource;
import org.glassfish.jersey.jackson.JacksonFeature;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * JAX-RS Application entry point. Sets the base API path to /api/v1.
 *
 * Default JAX-RS lifecycle: a new resource class instance is created per request.
 * This means instance fields are not shared between requests, keeping resource
 * classes thread-safe by default. However, it also means in-memory data cannot
 * be stored in resource instances — it must live in a separate singleton (DataStore).
 * DataStore uses ConcurrentHashMap to safely handle concurrent read/write access
 * from multiple simultaneous requests without race conditions or data loss.
 *
 * Note: JacksonFeature must be registered here inside getClasses(). When getClasses()
 * returns a non-empty set, Jersey disables auto-discovery, so any feature registered
 * externally (e.g. in Main.java) is ignored. Jackson is the JSON provider that
 * serialises all response objects — without it every endpoint returns 500.
 */
@ApplicationPath("/api/v1")
public class SmartCampusApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        // JSON provider — must be here, not just in Main
        classes.add(JacksonFeature.class);

        // Resources
        classes.add(DiscoveryResource.class);
        classes.add(RoomResource.class);
        classes.add(SensorResource.class);

        // Exception mappers
        classes.add(RoomNotEmptyExceptionMapper.class);
        classes.add(LinkedResourceNotFoundExceptionMapper.class);
        classes.add(SensorUnavailableExceptionMapper.class);
        classes.add(GlobalExceptionMapper.class);

        // Filters
        classes.add(LoggingFilter.class);

        return classes;
    }
}
