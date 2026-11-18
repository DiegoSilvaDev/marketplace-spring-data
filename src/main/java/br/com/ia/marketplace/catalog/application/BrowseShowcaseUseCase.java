package br.com.ia.marketplace.catalog.application;

import br.com.ia.marketplace.catalog.application.dto.EventOutput;
import br.com.ia.marketplace.catalog.domain.Event;
import br.com.ia.marketplace.catalog.domain.EventMetadataRepository;
import br.com.ia.marketplace.catalog.domain.EventRepository;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class BrowseShowcaseUseCase {
    private static final Logger logger = LoggerFactory.getLogger(BrowseShowcaseUseCase.class);

    private final EventRepository eventRepository;
    private final EventEnricher eventEnricher;

    public BrowseShowcaseUseCase(EventRepository eventRepository, EventEnricher eventEnricher) {
        this.eventRepository = eventRepository;
        this.eventEnricher = eventEnricher;
    }

    public List<EventOutput> execute(){
        var futures = eventRepository.findAll().stream().map(eventEnricher::enrich).toList();

        var events = futures.stream()
                .map(CompletableFuture::join)
                .map(EventOutput::from)
                .toList();

        logger.info("{} events enriched", futures);

        return events;
    }


}
