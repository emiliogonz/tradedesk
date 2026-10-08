package com.example.tradedesk.controller;

import com.example.tradedesk.dto.MarkResponse;
import com.example.tradedesk.dto.SetMarkRequest;
import com.example.tradedesk.service.MarkService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/marks")
public class MarkController {

    private final MarkService markService;

    public MarkController(MarkService markService) {
        this.markService = markService;
    }

    @PutMapping("/{instrumentId}")
    public MarkResponse setMark(@PathVariable Long instrumentId,
                                @Valid @RequestBody SetMarkRequest request) {
        return markService.setMark(instrumentId, request.rate());
    }
}