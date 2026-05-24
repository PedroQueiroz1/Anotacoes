package com.tasknotes.notes.service;

import com.tasknotes.notes.dto.NoteTagRequest;
import com.tasknotes.notes.dto.NoteTagResponse;
import com.tasknotes.notes.model.NoteTag;
import com.tasknotes.notes.repository.NoteTagRepository;
import com.tasknotes.shared.exception.ResourceNotFoundException;
import com.tasknotes.users.model.AppUser;
import com.tasknotes.users.service.SecurityHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NoteTagService {

    private final NoteTagRepository tagRepository;
    private final SecurityHelper securityHelper;

    public NoteTagService(NoteTagRepository tagRepository, SecurityHelper securityHelper) {
        this.tagRepository = tagRepository;
        this.securityHelper = securityHelper;
    }

    @Transactional(readOnly = true)
    public List<NoteTagResponse> listTags() {
        Long ownerId = securityHelper.currentUserId();
        return tagRepository.findByOwnerIdOrderByNameAsc(ownerId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public NoteTagResponse createTag(NoteTagRequest request) {
        AppUser owner = securityHelper.currentAppUser();
        NoteTag tag = new NoteTag();
        tag.setName(request.name().trim());
        tag.setColor(request.color());
        tag.setOwner(owner);
        return toResponse(tagRepository.save(tag));
    }

    @Transactional
    public void deleteTag(Long id) {
        Long ownerId = securityHelper.currentUserId();
        NoteTag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag não encontrada: " + id));
        if (!ownerId.equals(tag.getOwner().getId())) {
            throw new ResourceNotFoundException("Tag não encontrada: " + id);
        }
        tagRepository.deleteById(id);
    }

    public NoteTagResponse toResponse(NoteTag tag) {
        return new NoteTagResponse(tag.getId(), tag.getName(), tag.getColor());
    }
}
