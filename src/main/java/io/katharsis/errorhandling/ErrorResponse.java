package io.katharsis.errorhandling;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.katharsis.response.BaseResponse;

import java.util.Objects;

public final class ErrorResponse implements BaseResponse<Iterable<ErrorData>> {

    private static final String ERRORS = "errors";

    private final Iterable<ErrorData> data;
    private final int httpStatus;

    public ErrorResponse(Iterable<ErrorData> data, int httpStatus) {
        this.data = data;
        this.httpStatus = httpStatus;
    }

    @Override
    public int getHttpStatus() {
        return httpStatus;
    }

    @Override
    @JsonProperty(ERRORS)
    public final Iterable<ErrorData> getData() {
        return data;
    }

    public static ErrorResponseBuilder builder() {
        return new ErrorResponseBuilder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ErrorResponse)) return false;
        ErrorResponse that = (ErrorResponse) o;
        return Objects.equals(httpStatus, that.httpStatus) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, httpStatus);
    }
}