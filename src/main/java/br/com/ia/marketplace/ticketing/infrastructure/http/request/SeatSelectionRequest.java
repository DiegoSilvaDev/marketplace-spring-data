package br.com.ia.marketplace.ticketing.infrastructure.http.request;

import br.com.ia.marketplace.ticketing.domain.SeatId;

public record SeatSelectionRequest(String id) {
    public SeatId toIpunt(){
        return new SeatId(id);
    }

}
