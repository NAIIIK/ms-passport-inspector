package com.example.passportinspector.integration.smev;

import com.example.passportinspector.integration.dto.SmevRequestDto;
import com.example.passportinspector.integration.dto.SmevResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Component
@FeignClient(name = "smev-check-client", url = "${smev.client.url}")
public interface SmevCheckClient {
    @PostMapping(
            value = "/smev-api/v1/CheckPassport",
            consumes = "application/json",
            produces = "application/json"
    )
    SmevResponseDto checkPassport(
            @RequestHeader("x-token") String token,
            @RequestBody SmevRequestDto request
    );
}
