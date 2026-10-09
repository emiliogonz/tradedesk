package com.example.tradedesk.controller;

import com.example.tradedesk.dto.PnlSnapshotResponse;
import com.example.tradedesk.service.PnlSnapshotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/pnl")
public class PnlController {

    private final PnlSnapshotService snapshotService;

    public PnlController(PnlSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @GetMapping
    public List<PnlSnapshotResponse> history(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return snapshotService.list(from, to);
    }
}