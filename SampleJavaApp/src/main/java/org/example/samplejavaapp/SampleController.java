package org.example.samplejavaapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SampleController {

    private static final Logger log = LoggerFactory.getLogger(SampleController.class);

    @GetMapping()
    public String sampleRestAPI() {
        log.info("This instance was used!");
        return "Hello World!";
    }
}
