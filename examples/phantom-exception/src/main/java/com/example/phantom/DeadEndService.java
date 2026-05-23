package com.example.phantom;

import org.springframework.stereotype.Service;

@Service
public class DeadEndService {

    private final WorkerService service;
    private final ConfigService config;

    public DeadEndService(WorkerService service, ConfigService config) {
        this.service = service;
        this.config = config;
    }

    public void process() {
        boolean flagA = config.isFlagAEnabled();
        if (!flagA) {
            return; // Early exit
        }
        
        boolean flagB = config.isFlagBEnabled();
        if (flagB) {
            service.doThing();
        } else {
            service.doOtherThing();
        }
    }
}

interface WorkerService {
    void doThing();
    void doOtherThing();
}

interface ConfigService {
    boolean isFlagAEnabled();
    boolean isFlagBEnabled();
}
