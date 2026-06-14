package com.tasknotes.dictionary.service;

import com.tasknotes.dictionary.dto.DictionaryEntryRequest;
import com.tasknotes.dictionary.dto.DictionaryEntryResponse;
import com.tasknotes.dictionary.model.DictionaryEntry;
import com.tasknotes.dictionary.repository.DictionaryEntryRepository;
import com.tasknotes.shared.exception.BusinessException;
import com.tasknotes.users.model.AppUser;
import com.tasknotes.users.service.SecurityHelper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class DictionaryService {

    private static final String DUPLICATE_MESSAGE = "Esta palavra já está cadastrada no dicionário.";

    private final DictionaryEntryRepository repository;
    private final SecurityHelper securityHelper;

    public DictionaryService(DictionaryEntryRepository repository, SecurityHelper securityHelper) {
        this.repository = repository;
        this.securityHelper = securityHelper;
    }

    @Transactional(readOnly = true)
    public List<DictionaryEntryResponse> list() {
        Long userId = securityHelper.currentUserId();
        return repository.findByUserIdOrderByNormalizedTermAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public DictionaryEntryResponse create(DictionaryEntryRequest request) {
        AppUser user = securityHelper.currentAppUser();

        String term = request.term().trim();
        String definition = request.definition().trim();
        String normalizedTerm = normalize(term);

        if (term.isEmpty()) {
            throw new BusinessException("A palavra/termo é obrigatória.");
        }
        if (definition.isEmpty()) {
            throw new BusinessException("A descrição é obrigatória.");
        }
        if (repository.existsByUserIdAndNormalizedTerm(user.getId(), normalizedTerm)) {
            throw new BusinessException(DUPLICATE_MESSAGE);
        }

        DictionaryEntry entry = new DictionaryEntry();
        entry.setUser(user);
        entry.setTerm(term);
        entry.setNormalizedTerm(normalizedTerm);
        entry.setDefinition(definition);

        try {
            DictionaryEntry saved = repository.save(entry);
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            // Safety net for concurrent inserts of the same term (unique constraint)
            throw new BusinessException(DUPLICATE_MESSAGE);
        }
    }

    private String normalize(String term) {
        return term.trim().toLowerCase(Locale.ROOT);
    }

    private DictionaryEntryResponse toResponse(DictionaryEntry e) {
        return new DictionaryEntryResponse(e.getUuid(), e.getTerm(), e.getDefinition());
    }
}
