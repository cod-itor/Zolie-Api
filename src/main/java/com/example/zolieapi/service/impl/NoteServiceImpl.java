package com.example.zolieapi.service.impl;

import com.example.zolieapi.common.Texts;
import com.example.zolieapi.dto.response.NoteResponse;
import com.example.zolieapi.dto.response.UserSummaryResponse;
import com.example.zolieapi.mapper.UserMapper;
import com.example.zolieapi.model.UserNote;
import com.example.zolieapi.repository.UserNoteRepository;
import com.example.zolieapi.service.NoteService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NoteServiceImpl implements NoteService {

    private static final int MAX_TEXT = 60;
    private static final Duration LIFETIME = Duration.ofHours(24);

    private final UserNoteRepository noteRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public NoteResponse setMine(UUID userId, String text) {
        Instant now = Instant.now();
        UserNote note = noteRepository.findById(userId).orElseGet(() -> UserNote.builder().userId(userId).build());
        note.setText(Texts.required(text, MAX_TEXT, "text"));
        note.setCreatedAt(now);
        note.setExpiresAt(now.plus(LIFETIME));
        return toResponses(userId, List.of(noteRepository.saveAndFlush(note))).get(0);
    }

    @Override
    @Transactional
    public void deleteMine(UUID userId) {
        if (noteRepository.existsById(userId)) {
            noteRepository.deleteById(userId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NoteResponse> feed(UUID userId) {
        return toResponses(userId, noteRepository.findFeed(userId));
    }

    private List<NoteResponse> toResponses(UUID viewerId, List<UserNote> notes) {
        Map<UUID, UserSummaryResponse> users = userMapper.summariesById(notes.stream().map(UserNote::getUserId).toList());
        return notes.stream()
                .map(note -> new NoteResponse(users.get(note.getUserId()), note.getText(),
                        note.getUserId().equals(viewerId), note.getCreatedAt(), note.getExpiresAt()))
                .toList();
    }
}
