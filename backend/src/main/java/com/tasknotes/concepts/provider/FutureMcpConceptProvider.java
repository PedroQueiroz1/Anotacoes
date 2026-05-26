package com.tasknotes.concepts.provider;

import com.tasknotes.concepts.dto.ConceptSuggestionResponse;
import org.springframework.stereotype.Component;

import java.util.Optional;

// Stub for future MCP-based concept provider. Disabled until MCP integration is implemented.
@Component
public class FutureMcpConceptProvider implements ProgrammingConceptProvider {

    @Override
    public boolean isEnabled() { return false; }

    @Override
    public Optional<ConceptSuggestionResponse> explain(String normalizedTerm, ConceptLookupContext context) {
        return Optional.empty();
    }
}
