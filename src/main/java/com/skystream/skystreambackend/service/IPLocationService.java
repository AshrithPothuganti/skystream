package com.skystream.skystreambackend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class IPLocationService {

    private final RestTemplate rest = new RestTemplate();

    @SuppressWarnings("unchecked")
    public Map<String, Object> lookup(String ip) {
        try {
            // Using free API: ipapi.co
            String url = "https://ipapi.co/" + ip + "/json/";

            Map<String, Object> resp = rest.getForObject(url, Map.class);

            return resp != null ? resp : Map.of("error", "no-response");

        } catch (Exception ex) {
            return Map.of(
                    "error", "ip-lookup-failed",
                    "message", ex.getMessage()
            );
        }
    }
}
