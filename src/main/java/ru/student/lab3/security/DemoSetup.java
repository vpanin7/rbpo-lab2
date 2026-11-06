package ru.student.lab3.security;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration
@Profile("demo")
public class DemoSetup {
    @Bean
    ApplicationRunner demoAccounts(Accounts accounts, Environment env) {
        return args -> accounts.setupDemo(env.getRequiredProperty("DEMO_ADMIN_PASSWORD"),
                env.getRequiredProperty("DEMO_RESIDENT_PASSWORD"));
    }
}
