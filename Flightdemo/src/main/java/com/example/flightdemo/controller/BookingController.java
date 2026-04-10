package com.example.flightdemo.controller;

import com.example.flightdemo.dto.BookingRequest;
import com.example.flightdemo.dto.BookingResponse;
import com.example.flightdemo.exception.SagaException;
import com.example.flightdemo.orchestrator.BookingOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingOrchestrator orchestrator;

    @PostMapping
    public ResponseEntity<BookingResponse> book(@RequestBody BookingRequest request) {
        try {
            String sagaId = orchestrator.book(request);
            return ResponseEntity.ok(new BookingResponse(sagaId, "SUCCESS", "Booking completed successfully"));
        } catch (SagaException e) {
            return ResponseEntity.status(500).body(
                new BookingResponse(null, "FAILED",
                    "Booking failed at step [%s]: %s — compensating transactions executed."
                        .formatted(e.getFailedStep(), e.getMessage()))
            );
        }
    }
}
