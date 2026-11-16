package com.stride.service;

/** A rule was broken by the request (not a bug): the message is safe to show to the user. */
public class ServiceException extends RuntimeException {
    public ServiceException(String message) {
        super(message);
    }
}
