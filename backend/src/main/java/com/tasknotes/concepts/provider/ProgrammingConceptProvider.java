package com.tasknotes.concepts.provider;

import com.tasknotes.concepts.dto.ConceptSuggestionResponse;
import java.util.Optional;

public interface ProgrammingConceptProvider {
    Optional<ConceptSuggestionResponse> explain(String normalizedTerm, ConceptLookupContext context);
    boolean isEnabled();
}
