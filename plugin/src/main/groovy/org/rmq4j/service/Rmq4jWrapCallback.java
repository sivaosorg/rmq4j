package org.rmq4j.service;

import org.unify4j.model.response.WrapResponse;

/**
 * This interface defines a callback mechanism for handling RabbitMQ-related operations.
 * Implementations of this interface should provide logic for handling exceptions
 * that may occur during these operations and return a wrapped response.
 */
public interface Rmq4jWrapCallback {

    /**
     * This method is called when an exception occurs during a RabbitMQ operation.
     * Implementations should handle the exception and return an appropriate
     * WrapResponse object.
     *
     * @param response a WrapResponse object containing the result or error information, class {@link WrapResponse}
     */
    void onCallback(WrapResponse<?> response);
}
