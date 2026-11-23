package com.library.user_service.university;

/** What the library needs from the academic system: number, official name and course/department */
public record UniversityPerson(String registrationNumber, String name, String unit) {}
