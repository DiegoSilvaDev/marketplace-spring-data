package br.com.ia.marketplace.ticketing.infrastructure.persistence.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.time.Instant;

@RedisHash(value = "seat_lock", timeToLive = 30)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatLock {
    @Id
    private String id;

    @Indexed
    private String customerId;

    private Instant createdAt;
}

