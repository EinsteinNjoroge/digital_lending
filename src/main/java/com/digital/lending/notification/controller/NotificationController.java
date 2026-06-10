package com.digital.lending.notification.controller;

import com.digital.lending.notification.dto.NotificationAuditResponseDto;
import com.digital.lending.notification.dto.NotificationDispatchRequestDto;
import com.digital.lending.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification Infrastructure Engine", description = "Multi-channel unified layout interpolation messaging matrix")
public class NotificationController {

    private final NotificationService service;

    @PostMapping
    @Operation(summary = "Submit an outbound multi-channel message dispatch request")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Dispatch execution request successfully queued")
    })
    public ResponseEntity<Void> requestNotificationDispatch(@Valid @RequestBody NotificationDispatchRequestDto request) {
        service.processAndSendNotification(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @GetMapping
    @Operation(summary = "Fetch sent notifications in a paginated ledger layout",
            description = "Queries historical communication records across communication channels, destinations, execution windows, and status indicators.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved matched notification audit log entries matrix")
    })
    public ResponseEntity<Page<NotificationAuditResponseDto>> getNotificationAuditLogs(
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String recipient,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<NotificationAuditResponseDto> logs = service.getFilteredNotificationLogs(
                channel, recipient, status, fromDate, toDate, pageable
        );
        return ResponseEntity.ok(logs);
    }
}
