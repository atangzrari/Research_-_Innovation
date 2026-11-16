package com.stride.web;

import java.io.Serializable;

/** A one-time message shown at the top of the next page: success or a problem. */
public final class Flash implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean error;
    private final String text;
    private final String detail;

    public Flash(boolean error, String text, String detail) {
        this.error = error;
        this.text = text;
        this.detail = detail;
    }

    public boolean isError() { return error; }
    public String getText() { return text; }
    /** Extra line, e.g. a one-time temporary password. */
    public String getDetail() { return detail; }
}
