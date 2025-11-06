package com.sk.domain.mi.controller;


import com.sk.common.enums.ResultCode.Success;
import com.sk.common.model.BaseResponse;
import com.sk.common.model.BaseResponseFactory;
import com.sk.domain.mi.dto.response.autocomplete.AutocompleteTermData;
import com.sk.domain.mi.service.AutocompleteService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Profile("!dedicated")
@Tag(name = "14. Autocomplete API", description = "Autocomplete API 입니다.")
@Slf4j
@RestController
@RequestMapping("/api/v1/mi/autocomplete")
@RequiredArgsConstructor
public class AutocompleteController {
    private final AutocompleteService autocompleteService;

    @GetMapping("/topic")
    public BaseResponse<List<String>> autocomplete(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {

        List<AutocompleteTermData> results = autocompleteService.autocomplete(query, limit);

        return BaseResponseFactory.create(results.stream().map(AutocompleteTermData::getTerm).toList());
    }

    // 검색어 사용 기록
    @PostMapping("/topic/use")
    public BaseResponse<Void> recordUsage(@RequestParam String term) {
        autocompleteService.incrementFrequency(term);
        return BaseResponseFactory.create(Success.OK);
    }
}
