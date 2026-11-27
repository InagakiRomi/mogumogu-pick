package com.romi.mogumogu.dto.restaurant;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OverpassResponse(List<Element> elements, String remark) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Element(
            Double lat,
            Double lon,
            Center center,
            Map<String, String> tags) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Center(
            Double lat,
            Double lon) {
    }
}