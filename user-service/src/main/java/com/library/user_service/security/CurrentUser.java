package com.library.user_service.security;

import com.library.user_service.exception.ForbiddenException;
import com.library.user_service.exception.UnauthorizedException;

/**
 * Who is calling, taken from the X-User-Id / X-User-Role headers that the API Gateway sets
 * from the validated JWT (it discards any value sent by the client).
 * No headers = internal call from another service through Eureka (not reachable from outside Docker).
 */
public record CurrentUser(Long id, String role) {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";

    public static CurrentUser internal() {
        return new CurrentUser(null, null);
    }

    public boolean isInternal() {
        return id == null && role == null;
    }

    public boolean isLibrarian() {
        return "LIBRARIAN".equals(role);
    }

    /** For /me routes: there must be a logged-in user */
    public Long requireId() {
        if (id == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return id;
    }

    /** Librarian and internal calls see everything; a member only their own data */
    public void requireAccessTo(Long ownerId) {
        if (isInternal() || isLibrarian()) {
            return;
        }
        if (id == null || !id.equals(ownerId)) {
            throw new ForbiddenException("Access denied");
        }
    }
}
