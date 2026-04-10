package com.example.flightdemo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sagaId;
    private String userId;
    private String flightId;
    private String seatId;
    private String status; // RESERVED, RELEASED
}
