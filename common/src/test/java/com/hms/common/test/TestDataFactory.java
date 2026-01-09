package com.hms.common.test;

import net.datafaker.Faker;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Utility class for generating test data using DataFaker.
 * Provides methods for creating realistic test data for HMS entities.
 * 
 * <p>Usage:
 * <pre>
 * {@code
 * String email = TestDataFactory.email();
 * String fullName = TestDataFactory.fullName();
 * LocalDate birthDate = TestDataFactory.pastDate(18, 80);
 * }
 * </pre>
 */
public final class TestDataFactory {

    private static final Faker faker = new Faker(new Locale("en"));
    
    private TestDataFactory() {
        // Utility class - no instantiation
    }

    // ==================== COMMON ====================

    /**
     * Generate a random UUID string.
     */
    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate a random email address.
     */
    public static String email() {
        return faker.internet().emailAddress();
    }

    /**
     * Generate a unique email address with timestamp.
     */
    public static String uniqueEmail() {
        return "test." + System.currentTimeMillis() + "@hms-test.com";
    }

    /**
     * Generate a random password meeting typical requirements.
     */
    public static String password() {
        return faker.internet().password(8, 20, true, true, true);
    }

    /**
     * Generate a simple password for testing.
     */
    public static String simplePassword() {
        return "Test@123456";
    }

    /**
     * Generate a random phone number.
     */
    public static String phoneNumber() {
        return faker.phoneNumber().cellPhone();
    }

    /**
     * Generate a Vietnamese phone number format.
     */
    public static String vietnamesePhoneNumber() {
        return "0" + faker.number().digits(9);
    }

    // ==================== NAMES ====================

    /**
     * Generate a random full name.
     */
    public static String fullName() {
        return faker.name().fullName();
    }

    /**
     * Generate a random first name.
     */
    public static String firstName() {
        return faker.name().firstName();
    }

    /**
     * Generate a random last name.
     */
    public static String lastName() {
        return faker.name().lastName();
    }

    // ==================== ADDRESSES ====================

    /**
     * Generate a random street address.
     */
    public static String streetAddress() {
        return faker.address().streetAddress();
    }

    /**
     * Generate a random full address.
     */
    public static String fullAddress() {
        return faker.address().fullAddress();
    }

    /**
     * Generate a random city name.
     */
    public static String city() {
        return faker.address().city();
    }

    // ==================== DATES & TIMES ====================

    /**
     * Generate a past date between minYearsAgo and maxYearsAgo.
     */
    public static LocalDate pastDate(int minYearsAgo, int maxYearsAgo) {
        return LocalDate.now().minusYears(
            ThreadLocalRandom.current().nextInt(minYearsAgo, maxYearsAgo + 1)
        ).minusDays(
            ThreadLocalRandom.current().nextInt(0, 365)
        );
    }

    /**
     * Generate a birth date for an adult (18-80 years old).
     */
    public static LocalDate adultBirthDate() {
        return pastDate(18, 80);
    }

    /**
     * Generate a future date within the next N days.
     */
    public static LocalDate futureDate(int maxDaysAhead) {
        return LocalDate.now().plusDays(
            ThreadLocalRandom.current().nextInt(1, maxDaysAhead + 1)
        );
    }

    /**
     * Generate a future datetime within the next N days during business hours.
     */
    public static LocalDateTime futureAppointmentTime(int maxDaysAhead) {
        LocalDate date = futureDate(maxDaysAhead);
        int hour = ThreadLocalRandom.current().nextInt(8, 17); // 8 AM - 5 PM
        int minute = ThreadLocalRandom.current().nextInt(0, 4) * 15; // 0, 15, 30, 45
        return LocalDateTime.of(date, LocalTime.of(hour, minute));
    }

    /**
     * Generate a work schedule time.
     */
    public static LocalTime workStartTime() {
        int hour = ThreadLocalRandom.current().nextInt(7, 10);
        return LocalTime.of(hour, 0);
    }

    /**
     * Generate a work end time.
     */
    public static LocalTime workEndTime() {
        int hour = ThreadLocalRandom.current().nextInt(16, 19);
        return LocalTime.of(hour, 0);
    }

    // ==================== MEDICAL ====================

    /**
     * Generate a random blood type.
     */
    public static String bloodType() {
        String[] types = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        return types[ThreadLocalRandom.current().nextInt(types.length)];
    }

    /**
     * Generate random allergies text.
     */
    public static String allergies() {
        String[] allergies = {
            "None", "Penicillin", "Aspirin", "Sulfa drugs", 
            "Ibuprofen", "Latex", "Peanuts", "Shellfish"
        };
        return allergies[ThreadLocalRandom.current().nextInt(allergies.length)];
    }

    /**
     * Generate a random diagnosis.
     */
    public static String diagnosis() {
        return faker.medical().diseaseName();
    }

    /**
     * Generate a random medicine name.
     */
    public static String medicineName() {
        return faker.medical().medicineName();
    }

    /**
     * Generate random medical notes.
     */
    public static String medicalNotes() {
        return faker.lorem().paragraph(2);
    }

    /**
     * Generate a random symptom description.
     */
    public static String symptoms() {
        return faker.medical().symptoms();
    }

    // ==================== HR / DEPARTMENT ====================

    /**
     * Generate a random department name.
     */
    public static String departmentName() {
        String[] departments = {
            "Cardiology", "Neurology", "Orthopedics", "Pediatrics",
            "Emergency", "Radiology", "Oncology", "Dermatology",
            "Internal Medicine", "General Surgery", "Psychiatry"
        };
        return departments[ThreadLocalRandom.current().nextInt(departments.length)];
    }

    /**
     * Generate a unique department name.
     */
    public static String uniqueDepartmentName() {
        return departmentName() + " " + System.currentTimeMillis() % 10000;
    }

    /**
     * Generate a random job title.
     */
    public static String jobTitle() {
        String[] titles = {
            "Doctor", "Nurse", "Receptionist", "Lab Technician",
            "Pharmacist", "Radiologist", "Surgeon", "Administrator"
        };
        return titles[ThreadLocalRandom.current().nextInt(titles.length)];
    }

    // ==================== NUMBERS ====================

    /**
     * Generate a random positive amount for billing.
     */
    public static double amount() {
        return Math.round(ThreadLocalRandom.current().nextDouble(50, 5000) * 100.0) / 100.0;
    }

    /**
     * Generate a random positive integer in range.
     */
    public static int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /**
     * Generate a random quantity (1-100).
     */
    public static int quantity() {
        return randomInt(1, 100);
    }

    // ==================== GENDER ====================

    /**
     * Generate a random gender.
     */
    public static String gender() {
        return ThreadLocalRandom.current().nextBoolean() ? "MALE" : "FEMALE";
    }

    // ==================== TEXT ====================

    /**
     * Generate random lorem ipsum text.
     */
    public static String paragraph() {
        return faker.lorem().paragraph();
    }

    /**
     * Generate a random sentence.
     */
    public static String sentence() {
        return faker.lorem().sentence();
    }

    /**
     * Generate random words.
     */
    public static String words(int count) {
        return faker.lorem().words(count).toString();
    }

    /**
     * Get the underlying Faker instance for custom usage.
     */
    public static Faker faker() {
        return faker;
    }
}
