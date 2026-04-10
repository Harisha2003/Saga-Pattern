package com.example.flightdemo.dto;

// record auto-generates constructor, accessors, equals, hashCode, toString
public record BookingRequest(String userId, String flightId, String seatId, double amount) {}
