package com.adminvisitor.controller;

import com.adminvisitor.dto.requestdto.UpdateVisitorRequest;
import com.adminvisitor.dto.responsedto.VisitorResponse;
import com.adminvisitor.service.VisitorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/visitors")
@RequiredArgsConstructor
public class VisitorController {

    private final VisitorService visitorService;

    @GetMapping("/{visitorId}")
    public ResponseEntity<VisitorResponse> getVisitor(
            @PathVariable String visitorId) {

        VisitorResponse response =
                visitorService.getVisitor(visitorId);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{visitorId}")
    public ResponseEntity<VisitorResponse> updateVisitor(
            @PathVariable String visitorId,
            @Valid @RequestBody UpdateVisitorRequest request) {

        VisitorResponse response =
                visitorService.updateVisitor(visitorId, request);

        return ResponseEntity.ok(response);
    }
}