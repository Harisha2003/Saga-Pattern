package com.example.flightdemo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyPoints {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sagaId;
    private String userId;
    private int points;
    private String status; // AWARDED, REVERSED
}
