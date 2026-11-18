package com.example.zolieapi.controller;

import com.example.zolieapi.common.CursorPage;
import com.example.zolieapi.dto.request.DatingActionRequest;
import com.example.zolieapi.dto.request.DatingIntroRequest;
import com.example.zolieapi.dto.request.DatingLocationRequest;
import com.example.zolieapi.dto.request.DatingPhotosRequest;
import com.example.zolieapi.dto.request.DatingPreferencesRequest;
import com.example.zolieapi.dto.request.DatingProfileRequest;
import com.example.zolieapi.dto.request.DatingStatusRequest;
import com.example.zolieapi.dto.request.PromptAnswersRequest;
import com.example.zolieapi.dto.response.CountResponse;
import com.example.zolieapi.dto.response.DatingPromptResponse;
import com.example.zolieapi.dto.response.DatingLikeResponse;
import com.example.zolieapi.dto.response.DatingPreferencesResponse;
import com.example.zolieapi.dto.response.DatingProfileResponse;
import com.example.zolieapi.dto.response.DiscoveryResponse;
import com.example.zolieapi.dto.response.EligibilityResponse;
import com.example.zolieapi.dto.response.MatchResponse;
import com.example.zolieapi.dto.response.ReceivedLikeResponse;
import com.example.zolieapi.security.CurrentUser;
import com.example.zolieapi.service.DatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dating")
@RequiredArgsConstructor
@Tag(name = "14. Dating (18+)")
public class DatingController {

    private final DatingService datingService;
    private final CurrentUser currentUser;

    @GetMapping("/eligibility")
    @Operation(summary = "Check whether I can use Dating: under_18, no_profile, paused or off")
    public EligibilityResponse eligibility() {
        return datingService.eligibility(currentUser.id());
    }

    @GetMapping("/profile")
    @Operation(summary = "Get my dating profile")
    public DatingProfileResponse getProfile() {
        return datingService.getProfile(currentUser.id());
    }

    @PutMapping("/profile")
    @Operation(summary = "Create or update my dating profile (18+ only)")
    public DatingProfileResponse saveProfile(@Valid @RequestBody DatingProfileRequest request) {
        return datingService.saveProfile(currentUser.id(), request);
    }

    @PutMapping("/profile/photos")
    @Operation(summary = "Set my dating photos in order (up to 6)")
    public DatingProfileResponse replacePhotos(@Valid @RequestBody DatingPhotosRequest request) {
        return datingService.replacePhotos(currentUser.id(), request.mediaIds());
    }

    @GetMapping("/prompts")
    @Operation(summary = "List the prompt questions in English and Khmer")
    public List<DatingPromptResponse> listPrompts() {
        return datingService.listPrompts();
    }

    @PutMapping("/profile/prompts")
    @Operation(summary = "Set my prompt answers in order (up to 3)")
    public DatingProfileResponse replacePromptAnswers(@Valid @RequestBody PromptAnswersRequest request) {
        return datingService.replacePromptAnswers(currentUser.id(), request.answers());
    }

    @GetMapping("/likes/received/count")
    @Operation(summary = "How many people sparked me this week and are waiting for an answer")
    public CountResponse countReceivedThisWeek() {
        return datingService.countReceivedThisWeek(currentUser.id());
    }

    @PatchMapping("/profile/status")
    @Operation(summary = "Turn Dating on, pause it or turn it off")
    public DatingProfileResponse changeStatus(@Valid @RequestBody DatingStatusRequest request) {
        return datingService.changeStatus(currentUser.id(), request.status());
    }

    @GetMapping("/preferences")
    @Operation(summary = "Get who I want to meet")
    public DatingPreferencesResponse getPreferences() {
        return datingService.getPreferences(currentUser.id());
    }

    @PutMapping("/preferences")
    @Operation(summary = "Set genders, age range (18+) and distance (1 to 200 km)")
    public DatingPreferencesResponse savePreferences(@Valid @RequestBody DatingPreferencesRequest request) {
        return datingService.savePreferences(currentUser.id(), request);
    }

    @PutMapping("/location")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Save my approximate location (rounded to about 1 km, never returned)")
    public void updateLocation(@Valid @RequestBody DatingLocationRequest request) {
        datingService.updateLocation(currentUser.id(), request);
    }

    @GetMapping("/discovery")
    @Operation(summary = "Get today's remaining picks (5 a day), with distance shown as a band")
    public DiscoveryResponse discover(@RequestParam(required = false) Integer limit) {
        return datingService.discover(currentUser.id(), limit);
    }

    @PostMapping("/likes")
    @Operation(summary = "Like someone; returns matched=true when they already liked me")
    public DatingLikeResponse like(@Valid @RequestBody DatingActionRequest request) {
        return datingService.like(currentUser.id(), request.toUserId());
    }

    @PostMapping("/passes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Pass on someone so they are not shown again")
    public void pass(@Valid @RequestBody DatingActionRequest request) {
        datingService.pass(currentUser.id(), request.toUserId());
    }

    @PostMapping("/intros")
    @Operation(summary = "Send the one intro message allowed per person (409 on a second try)")
    public DatingLikeResponse sendIntro(@Valid @RequestBody DatingIntroRequest request) {
        return datingService.sendIntro(currentUser.id(), request.toUserId(), request.message(),
                request.promptAnswerId());
    }

    @GetMapping("/likes/received")
    @Operation(summary = "List people who liked me or sent me an intro")
    public CursorPage<ReceivedLikeResponse> listReceivedLikes(@RequestParam(required = false) String cursor,
                                                              @RequestParam(required = false) Integer limit) {
        return datingService.listReceivedLikes(currentUser.id(), false, cursor, limit);
    }

    @GetMapping("/intros/received")
    @Operation(summary = "List intros waiting for my answer")
    public CursorPage<ReceivedLikeResponse> listReceivedIntros(@RequestParam(required = false) String cursor,
                                                               @RequestParam(required = false) Integer limit) {
        return datingService.listReceivedLikes(currentUser.id(), true, cursor, limit);
    }

    @GetMapping("/matches")
    @Operation(summary = "List my matches, newest first")
    public CursorPage<MatchResponse> listMatches(@RequestParam(required = false) String cursor,
                                                 @RequestParam(required = false) Integer limit) {
        return datingService.listMatches(currentUser.id(), cursor, limit);
    }

    @DeleteMapping("/matches/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Unmatch: closes the dating chat for both people")
    public void unmatch(@PathVariable UUID id) {
        datingService.unmatch(currentUser.id(), id);
    }
}
