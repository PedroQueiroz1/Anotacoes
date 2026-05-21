package com.tasknotes.concepts.dto;

import com.tasknotes.concepts.model.ProgrammingConcept;

public record ConceptSuggestionResponse(
        boolean found,
        String source,
        ConceptItem concept
) {
    public record ConceptItem(String uuid, String term, String summary) {}

    public static ConceptSuggestionResponse notFound() {
        return new ConceptSuggestionResponse(false, null, null);
    }

    public static ConceptSuggestionResponse of(ProgrammingConcept c, String source) {
        return new ConceptSuggestionResponse(
                true,
                source,
                new ConceptItem(c.getUuid(), c.getTerm(), c.getSummary())
        );
    }
}
