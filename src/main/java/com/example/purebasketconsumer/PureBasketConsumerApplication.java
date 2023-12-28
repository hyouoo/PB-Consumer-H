package com.example.purebasketconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class PureBasketConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(PureBasketConsumerApplication.class, args);
    }

}
