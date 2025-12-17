package com.francofral.opentelemetry.person;

public record PersonDto(
        String name,
        String lastname,
        int age
) {
}
