package com.cowave.commons.client.http.options;

import com.cowave.commons.client.http.response.HttpResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.cowave.commons.client.http.constants.HttpHeader.X_Request_ID;

/**
 *
 * @author shanhuiming
 *
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/mock")
public class OptionsMockController {

    private final OptionsMockService optionsMockService;

    @GetMapping("/options")
    public String options() {
        HttpResponse<Void> httpResponse = optionsMockService.options();
        return httpResponse.getRemoteHeaders().get(X_Request_ID).get(0);
    }
}
