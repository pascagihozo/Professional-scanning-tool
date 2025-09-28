package com.lci.scannerdesktop.api;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.io.InputStream;

@Controller
@RequestMapping("/ui")
public class LocalUiController {

    @GetMapping
    public ResponseEntity<byte[]> index() throws IOException {
        ClassPathResource res = new ClassPathResource("ui/index.html");
        byte[] body;
        try (InputStream is = res.getInputStream()) {
            body = is.readAllBytes();
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(body);
    }
}


