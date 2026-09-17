//package com.adminvisitor.controller;
//
//import com.adminvisitor.dto.requestdto.VisitorRequest;
//import com.adminvisitor.dto.responsedto.VisitorResponse;
//import com.adminvisitor.service.VisitorService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/visitors")
//@RequiredArgsConstructor
//public class VisitorController {
//
//    private final VisitorService visitorService;
//
//    @PostMapping
//    public ResponseEntity<VisitorResponse> createVisitor(
//            @Valid @RequestBody VisitorRequest request) {
//
//        VisitorResponse response =
//                visitorService.createVisitor(request);
//
//        return ResponseEntity
//                .status(HttpStatus.CREATED)
//                .body(response);
//    }
//}