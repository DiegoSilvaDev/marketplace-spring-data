package br.com.ia.marketplace.catalog.infrastructure.persistence.repository;

import br.com.ia.marketplace.catalog.infrastructure.persistence.entity.EventMetadata;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface EventMetadataEntityRepository extends MongoRepository<EventMetadata, String> {
}
