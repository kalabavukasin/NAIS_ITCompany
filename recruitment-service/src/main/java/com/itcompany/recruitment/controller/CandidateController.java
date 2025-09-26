package com.itcompany.recruitment.controller;

import com.itcompany.recruitment.model.Candidate;
import com.itcompany.recruitment.dto.CandidateSearchRequest;
import com.itcompany.recruitment.service.CandidateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/candidates")
@RequiredArgsConstructor
@Tag(name = "Candidates", description = "Candidate management APIs")
public class CandidateController {

}
