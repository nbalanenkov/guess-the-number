package com.task.guessnumber.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;

@Configuration
public class ApplicationConfig {

    @Bean
    public Random random() {
        return new Random();
    }

    @Value("${round.delay}")
    private int roundDelay;

    @Bean
    public int roundDelay() {
        return roundDelay;
    }
}
