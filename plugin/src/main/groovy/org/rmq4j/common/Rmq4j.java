package org.rmq4j.common;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.unify4j.common.UniqueId4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class Rmq4j {

    /**
     * @return the HTTP servlet request, class {@link HttpServletRequest}
     */
    public static HttpServletRequest getRequest() {
        ServletRequestAttributes s = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return s.getRequest();
    }

    /**
     * Retrieves the current session ID from the request context.
     * <p>
     * This method accesses the current request attributes from the RequestContextHolder
     * and extracts the session ID associated with the current request. This is useful
     * for tracking the session of the user making the request, especially in web
     * applications where session management is crucial for user authentication and
     * maintaining user state across multiple requests.
     *
     * @return the session ID of the current request, or null if no session is associated with the current request context
     */
    public static String getCurrentSessionId() {
        try {
            ServletRequestAttributes s = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return s.getSessionId();
        } catch (IllegalStateException e) {
            return String.valueOf(UniqueId4j.getUniqueId19());
        }
    }

    /**
     * Retrieves the session ID from the given HttpServletRequest.
     * <p>
     * This method gets the current HttpSession associated with the request,
     * and then extracts the session ID from it. If there is no current session
     * and create is false, it returns null.
     *
     * @param request the HttpServletRequest from which to retrieve the session ID
     * @return the session ID, or null if there is no current session
     */
    public static String getSessionId(HttpServletRequest request) {
        if (request == null) {
            return String.valueOf(UniqueId4j.getUniqueId19());
        }
        HttpSession session = request.getSession(false); // Pass false to prevent creating a new session if one does not exist
        return (session != null) ? session.getId() : null;
    }
}
