package com.skystream.skystreambackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// explicitly point to the main application class
@SpringBootTest(classes = SkystreamBackendApplication.class)
public class SkystreamBackendApplicationTests {

    @Test
    void contextLoads() {
        // passes if Spring context starts
    }
}
