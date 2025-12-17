package com.francofral.opentelemetry.person;

public record Person(
        String id,
        String name,
        String lastname,
        int age
) {
}
