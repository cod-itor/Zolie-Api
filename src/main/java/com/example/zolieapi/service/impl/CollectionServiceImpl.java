package com.example.zolieapi.service.impl;

import com.example.zolieapi.common.Cursor;
import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.common.Texts;
import com.example.zolieapi.dto.request.CollectionItemRequest;
import com.example.zolieapi.dto.response.CollectionResponse;
import com.example.zolieapi.dto.response.PostResponse;
import com.example.zolieapi.dto.response.ReelResponse;
import com.example.zolieapi.dto.response.SavedItemResponse;
import com.example.zolieapi.exception.ApiException;
import com.example.zolieapi.mapper.PostMapper;
import com.example.zolieapi.mapper.ReelMapper;
import com.example.zolieapi.model.CollectionItem;
import com.example.zolieapi.model.Post;
import com.example.zolieapi.model.Reel;
import com.example.zolieapi.model.SavedCollection;
import com.example.zolieapi.repository.CollectionItemRepository;
import com.example.zolieapi.repository.CollectionRepository;
import com.example.zolieapi.repository.CollectionRepository.ItemCountRow;
import com.example.zolieapi.repository.PostRepository;
import com.example.zolieapi.repository.ReelRepository;
import com.example.zolieapi.service.CollectionService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CollectionServiceImpl implements CollectionService {

    private static final int MAX_NAME = 60;
    private static final int MAX_COLLECTIONS = 50;

    private final CollectionRepository collectionRepository;
    private final CollectionItemRepository itemRepository;
    private final PostRepository postRepository;
    private final ReelRepository reelRepository;
    private final PostMapper postMapper;
    private final ReelMapper reelMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CollectionResponse> list(UUID userId) {
        List<SavedCollection> collections = collectionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        if (collections.isEmpty()) {
            return List.of();
        }
        Map<UUID, Long> counts = collectionRepository.countItems(collections.stream().map(SavedCollection::getId).toList())
                .stream().collect(Collectors.toMap(ItemCountRow::getCollectionId, ItemCountRow::getTotal));
        return collections.stream()
                .map(collection -> toResponse(collection, counts.getOrDefault(collection.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional
    public CollectionResponse create(UUID userId, String name) {
        if (collectionRepository.countByUserId(userId) >= MAX_COLLECTIONS) {
            throw ApiException.rule("You can have up to " + MAX_COLLECTIONS + " collections.");
        }
        String cleaned = requireFreeName(userId, name);
        SavedCollection collection = collectionRepository.saveAndFlush(
                SavedCollection.builder().userId(userId).name(cleaned).build());
        return toResponse(collection, 0);
    }

    @Override
    @Transactional
    public CollectionResponse rename(UUID userId, UUID collectionId, String name) {
        SavedCollection collection = requireOwn(userId, collectionId);
        if (!collection.getName().equals(Texts.clean(name))) {
            collection.setName(requireFreeName(userId, name));
        }
        long count = collectionRepository.countItems(List.of(collectionId)).stream()
                .mapToLong(ItemCountRow::getTotal).sum();
        return toResponse(collectionRepository.saveAndFlush(collection), count);
    }

    @Override
    @Transactional
    public void delete(UUID userId, UUID collectionId) {
        collectionRepository.delete(requireOwn(userId, collectionId));
    }

    @Override
    @Transactional
    public void addItem(UUID userId, UUID collectionId, CollectionItemRequest request) {
        requireOwn(userId, collectionId);
        if ((request.postId() == null) == (request.reelId() == null)) {
            throw ApiException.invalid("Send exactly one of postId or reelId");
        }
        if (request.postId() != null) {
            postRepository.findVisible(request.postId(), userId).orElseThrow(ApiException::notFound);
            postRepository.insertSave(request.postId(), userId);
            if (!itemRepository.existsByCollectionIdAndPostId(collectionId, request.postId())) {
                itemRepository.save(CollectionItem.builder().collectionId(collectionId).postId(request.postId()).build());
            }
            return;
        }
        reelRepository.findVisible(request.reelId(), userId).orElseThrow(ApiException::notFound);
        reelRepository.insertSave(request.reelId(), userId);
        if (!itemRepository.existsByCollectionIdAndReelId(collectionId, request.reelId())) {
            itemRepository.save(CollectionItem.builder().collectionId(collectionId).reelId(request.reelId()).build());
        }
    }

    @Override
    @Transactional
    public void removeItem(UUID userId, UUID collectionId, UUID postId, UUID reelId) {
        requireOwn(userId, collectionId);
        if (postId != null) {
            itemRepository.removePost(collectionId, postId);
        }
        if (reelId != null) {
            itemRepository.removeReel(collectionId, reelId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<SavedItemResponse> listItems(UUID userId, UUID collectionId, String cursor, Integer limit) {
        requireOwn(userId, collectionId);
        int size = CursorPage.limit(limit);
        Cursor from = Cursor.decode(cursor);
        return CursorPage.of(itemRepository.findPage(collectionId, from.time(), from.id(), size + 1), size,
                item -> new Cursor(item.getAddedAt(), item.getId()), page -> toItems(userId, page));
    }

    private String requireFreeName(UUID userId, String name) {
        String cleaned = Texts.required(name, MAX_NAME, "name");
        if (collectionRepository.existsByUserIdAndName(userId, cleaned)) {
            throw ApiException.rule("You already have a collection with this name.");
        }
        return cleaned;
    }

    private SavedCollection requireOwn(UUID userId, UUID collectionId) {
        return collectionRepository.findByIdAndUserId(collectionId, userId).orElseThrow(ApiException::notFound);
    }

    private CollectionResponse toResponse(SavedCollection collection, long itemCount) {
        return new CollectionResponse(collection.getId(), collection.getName(), itemCount, collection.getCreatedAt());
    }

    private List<SavedItemResponse> toItems(UUID userId, List<CollectionItem> items) {
        List<Post> posts = items.stream().map(CollectionItem::getPostId).filter(Objects::nonNull)
                .map(postId -> postRepository.findVisible(postId, userId)).flatMap(Optional::stream).toList();
        List<Reel> reels = items.stream().map(CollectionItem::getReelId).filter(Objects::nonNull)
                .map(reelId -> reelRepository.findVisible(reelId, userId)).flatMap(Optional::stream).toList();
        Map<UUID, PostResponse> postResponses = postMapper.toResponses(userId, posts).stream()
                .collect(Collectors.toMap(PostResponse::id, Function.identity()));
        Map<UUID, ReelResponse> reelResponses = reelMapper.toResponses(userId, reels).stream()
                .collect(Collectors.toMap(ReelResponse::id, Function.identity()));
        return items.stream()
                .map(item -> item.getPostId() != null
                        ? new SavedItemResponse("post", postResponses.get(item.getPostId()), null, item.getAddedAt())
                        : new SavedItemResponse("reel", null, reelResponses.get(item.getReelId()), item.getAddedAt()))
                .filter(item -> item.post() != null || item.reel() != null)
                .toList();
    }
}
