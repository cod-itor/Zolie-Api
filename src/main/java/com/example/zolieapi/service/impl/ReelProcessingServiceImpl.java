package com.example.zolieapi.service.impl;

import com.example.zolieapi.config.ZolieProperties;
import com.example.zolieapi.model.MediaAsset;
import com.example.zolieapi.model.Reel;
import com.example.zolieapi.model.enums.MediaKind;
import com.example.zolieapi.model.enums.MediaPurpose;
import com.example.zolieapi.model.enums.MediaStatus;
import com.example.zolieapi.model.enums.ReelStatus;
import com.example.zolieapi.repository.MediaAssetRepository;
import com.example.zolieapi.repository.ReelRepository;
import com.example.zolieapi.service.PushService;
import com.example.zolieapi.service.ReelProcessingService;
import com.example.zolieapi.service.StorageService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReelProcessingServiceImpl implements ReelProcessingService {

    private static final int MAX_REEL_MS = 90_000;
    private static final long PROCESS_TIMEOUT_SECONDS = 300;
    private static final String OUTPUT_MIME = "video/mp4";
    private static final String THUMBNAIL_MIME = "image/jpeg";

    private final ReelRepository reelRepository;
    private final MediaAssetRepository mediaAssetRepository;
    private final StorageService storageService;
    private final PushService pushService;
    private final ZolieProperties properties;

    @Override
    @Transactional
    public boolean processNext() {
        List<Reel> next = reelRepository.lockNextToProcess(1);
        if (next.isEmpty()) {
            return false;
        }
        Reel reel = next.get(0);
        MediaAsset video = mediaAssetRepository.findById(reel.getVideoId()).orElseThrow();
        try {
            if (properties.storage().isConfigured()) {
                transcode(reel, video);
            }
            video.setStatus(MediaStatus.READY);
            reel.setStatus(ReelStatus.READY);
            pushService.send(reel.getAuthorId(), Map.of("type", "reel_ready", "reelId", reel.getId().toString()));
        } catch (IOException | InterruptedException | RuntimeException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Reel processing failed: reel={} reason={}", reel.getId(), e.getMessage());
            video.setStatus(MediaStatus.FAILED);
            reel.setStatus(ReelStatus.FAILED);
            pushService.send(reel.getAuthorId(), Map.of("type", "upload_failed", "reelId", reel.getId().toString()));
        }
        return true;
    }

    private void transcode(Reel reel, MediaAsset video) throws IOException, InterruptedException {
        Path workDir = Files.createTempDirectory("zolie-reel-");
        try {
            Path input = workDir.resolve("input");
            Path output = workDir.resolve("output.mp4");
            Path thumbnail = workDir.resolve("thumbnail.jpg");
            storageService.download(video.getObjectKey(), input);

            int durationMs = probeDurationMs(input);
            if (durationMs <= 0 || durationMs > MAX_REEL_MS) {
                throw new IllegalStateException("Video duration is outside the allowed range: " + durationMs + " ms");
            }
            run("ffmpeg", "-y", "-i", input.toString(), "-vf", "scale=-2:720", "-c:v", "libx264", "-preset", "veryfast",
                    "-crf", "26", "-c:a", "aac", "-b:a", "128k", "-movflags", "+faststart", output.toString());
            run("ffmpeg", "-y", "-ss", "0.5", "-i", output.toString(), "-frames:v", "1", "-q:v", "3",
                    thumbnail.toString());

            storageService.upload(output, video.getObjectKey(), OUTPUT_MIME);
            String thumbnailKey = video.getObjectKey() + ".thumbnail.jpg";
            storageService.upload(thumbnail, thumbnailKey, THUMBNAIL_MIME);

            video.setMimeType(OUTPUT_MIME);
            video.setSizeBytes(Files.size(output));
            video.setDurationMs(durationMs);
            video.setHeight(720);
            reel.setDurationMs(durationMs);
            reel.setThumbnailId(mediaAssetRepository.save(MediaAsset.builder()
                    .ownerId(reel.getAuthorId())
                    .objectKey(thumbnailKey)
                    .kind(MediaKind.IMAGE)
                    .purpose(MediaPurpose.REEL_THUMBNAIL)
                    .mimeType(THUMBNAIL_MIME)
                    .sizeBytes(Files.size(thumbnail))
                    .status(MediaStatus.READY)
                    .build()).getId());
        } finally {
            deleteRecursively(workDir);
        }
    }

    private int probeDurationMs(Path input) throws IOException, InterruptedException {
        String seconds = run("ffprobe", "-v", "error", "-show_entries", "format=duration", "-of",
                "default=noprint_wrappers=1:nokey=1", input.toString());
        return (int) Math.round(Double.parseDouble(seconds.trim()) * 1000);
    }

    private String run(String... command) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            throw new IllegalStateException(command[0] + " timed out");
        }
        if (process.exitValue() != 0) {
            throw new IllegalStateException(command[0] + " exited with code " + process.exitValue());
        }
        return output;
    }

    private void deleteRecursively(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        }
    }
}
