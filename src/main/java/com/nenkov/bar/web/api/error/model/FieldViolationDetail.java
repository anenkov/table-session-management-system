package com.nenkov.bar.web.api.error.model;

/**
 * A single field-level validation issue, attached to RFC7807 Problem Details as an extension
 * property.
 */
public record FieldViolationDetail(String field, String issue) {}
