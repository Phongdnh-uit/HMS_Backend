plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "HMS_BACKEND"

include(
    "config-server",
    "discovery-service",
    "common",
    "api-gateway",
    "medicine-service",
    "auth-service",
    "patient-service",
    "hr-service",
    "appointment-service",
    "medical-exam-service",
    "billing-service",
    "report-service",
    "notification-service"
)

