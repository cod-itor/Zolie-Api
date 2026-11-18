package com.example.zolieapi.controller;

import com.example.zolieapi.dto.response.PlaceResponse;
import com.example.zolieapi.service.PlaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/places")
@RequiredArgsConstructor
@Tag(name = "20. Places")
public class PlaceController {

    private final PlaceService placeService;

    @GetMapping
    @Operation(summary = "Search places by English or Khmer name for location tags")
    public List<PlaceResponse> search(@RequestParam(required = false) String q) {
        return placeService.search(q);
    }

    @GetMapping("/popular")
    @Operation(summary = "Places people love, for Explore")
    public List<PlaceResponse> listPopular() {
        return placeService.listPopular();
    }
}
