package br.com.ia.marketplace.ticketing.application;

import br.com.ia.marketplace.ticketing.domain.EventId;
import br.com.ia.marketplace.ticketing.domain.CustomerId;
import br.com.ia.marketplace.ticketing.domain.EventRepository;
import br.com.ia.marketplace.ticketing.domain.SeatAlreadyReservedException;
import br.com.ia.marketplace.ticketing.domain.SeatId;
import br.com.ia.marketplace.ticketing.domain.SeatNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class SelectSeatUseCase {
    private final EventRepository eventRepository;

    public SelectSeatUseCase(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public void execute(EventId eventId, SeatId seatId, CustomerId customerId) {
        if (!eventRepository.existsSeat(eventId, seatId)) {
            throw new SeatNotFoundException(eventId, seatId);
        }

        boolean lock = eventRepository.tryLockSeat(eventId, seatId, customerId);

        if (!lock) {
            throw new SeatAlreadyReservedException();
        }

        // Order, Payment, ...
    }
}
