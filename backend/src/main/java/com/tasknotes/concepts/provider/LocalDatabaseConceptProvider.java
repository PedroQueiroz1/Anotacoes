package com.tasknotes.concepts.provider;

import com.tasknotes.concepts.dto.ConceptSuggestionResponse;
import com.tasknotes.concepts.model.ProgrammingConcept;
import com.tasknotes.concepts.repository.ProgrammingConceptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LocalDatabaseConceptProvider implements ProgrammingConceptProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalDatabaseConceptProvider.class);
    private static final int CACHE_MAX_SIZE = 300;

    private final ProgrammingConceptRepository repository;
    private final ConcurrentHashMap<String, Optional<ConceptSuggestionResponse>> globalCache =
            new ConcurrentHashMap<>();

    public LocalDatabaseConceptProvider(ProgrammingConceptRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isEnabled() { return true; }

    @Override
    public Optional<ConceptSuggestionResponse> explain(String normalizedTerm, ConceptLookupContext context) {
        long start = System.currentTimeMillis();

        // 1. Global cache hit
        boolean globalAlreadyChecked = false;
        if (globalCache.containsKey(normalizedTerm)) {
            Optional<ConceptSuggestionResponse> cached = globalCache.get(normalizedTerm);
            if (cached.isPresent()) {
                return cached;
            }
            globalAlreadyChecked = true;
        }

        if (!globalAlreadyChecked) {
            // 2. Exact match — GLOBAL scope
            Optional<ProgrammingConcept> exact = repository.findByNormalizedTermAndScope(normalizedTerm, "GLOBAL");
            if (exact.isPresent()) {
                var resp = ConceptSuggestionResponse.of(exact.get(), "LOCAL");
                putGlobalCache(normalizedTerm, Optional.of(resp));
                log.info("concept_lookup_completed termNormalized={} source=LOCAL scope=GLOBAL found=true durationMs={}",
                        normalizedTerm, System.currentTimeMillis() - start);
                return Optional.of(resp);
            }

            // 3. Prefix match — GLOBAL scope
            var prefixMatches = repository.findByNormalizedTermStartingWithAndScope(
                    normalizedTerm, "GLOBAL", PageRequest.of(0, 1));
            if (!prefixMatches.isEmpty()) {
                var resp = ConceptSuggestionResponse.of(prefixMatches.get(0), "LOCAL");
                putGlobalCache(normalizedTerm, Optional.of(resp));
                log.info("concept_lookup_completed termNormalized={} source=LOCAL scope=GLOBAL found=true durationMs={}",
                        normalizedTerm, System.currentTimeMillis() - start);
                return Optional.of(resp);
            }

            putGlobalCache(normalizedTerm, Optional.empty());
        }

        // 4. Exact match — USER scope
        if (context.userId() != null) {
            Optional<ProgrammingConcept> userExact =
                    repository.findByNormalizedTermAndOwnerUserId(normalizedTerm, context.userId());
            if (userExact.isPresent()) {
                log.info("concept_lookup_completed termNormalized={} source=LOCAL scope=USER found=true durationMs={}",
                        normalizedTerm, System.currentTimeMillis() - start);
                return Optional.of(ConceptSuggestionResponse.of(userExact.get(), "LOCAL"));
            }
        }

        log.info("concept_lookup_not_found termNormalized={} durationMs={}",
                normalizedTerm, System.currentTimeMillis() - start);
        return Optional.empty();
    }

    private void putGlobalCache(String key, Optional<ConceptSuggestionResponse> value) {
        if (globalCache.size() >= CACHE_MAX_SIZE) globalCache.clear();
        globalCache.put(key, value);
    }
}
