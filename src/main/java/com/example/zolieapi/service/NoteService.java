package com.example.zolieapi.service;

import com.example.zolieapi.dto.response.NoteResponse;
import java.util.List;
import java.util.UUID;

public interface NoteService {

    NoteResponse setMine(UUID userId, String text);

    void deleteMine(UUID userId);

    List<NoteResponse> feed(UUID userId);
}
