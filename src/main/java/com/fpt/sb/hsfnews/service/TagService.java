package com.fpt.sb.hsfnews.service;

import com.fpt.sb.hsfnews.entity.Tag;
import com.fpt.sb.hsfnews.repository.TagRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagService {

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public List<Tag> findAllById(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return tagRepository.findAllById(ids);
    }
}

