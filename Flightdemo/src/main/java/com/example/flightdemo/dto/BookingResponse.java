package com.example.flightdemo.dto;

// record auto-generates constructor, accessors, equals, hashCode, toString
public record BookingResponse(String sagaId, String status, String message) {}
