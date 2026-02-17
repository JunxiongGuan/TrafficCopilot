package com.trafficcopilot.core;

import java.util.Set;

public class SubmissionCoreTest {

    public static void main(String[] args) {
        unsupportedCityShouldFail();
        drivingStateShouldReturnPending();
        missingIdentityShouldReturnRequiredFields();
        shenzhenRequestShouldBuildPayload();
        System.out.println("All SubmissionCore tests passed.");
    }

    static void unsupportedCityShouldFail() {
        SubmissionCore.SubmissionOrchestrator orchestrator = SubmissionCore.buildRuntimeCore();

        SubmissionCore.SubmissionRequest request = new SubmissionCore.SubmissionRequest(
                "beijing",
                false,
                new SubmissionCore.EvidencePayload(
                        "2026-01-01T08:00:00+08:00",
                        "北京市朝阳区XX路",
                        "京A12345",
                        "实线变道",
                        "file://evidence.mp4"
                ),
                new SubmissionCore.IdentityProfile("13800000000", "510101199001010011")
        );

        SubmissionCore.SubmissionResult result = orchestrator.prepare(request);
        assertEquals("failed", result.status());
    }

    static void drivingStateShouldReturnPending() {
        SubmissionCore.SubmissionOrchestrator orchestrator = SubmissionCore.buildRuntimeCore();

        SubmissionCore.SubmissionRequest request = new SubmissionCore.SubmissionRequest(
                "shenzhen",
                true,
                new SubmissionCore.EvidencePayload(
                        "2026-01-01T08:00:00+08:00",
                        "深圳市南山区XX大道",
                        "粤B12345",
                        "占用应急车道",
                        "file://evidence.mp4"
                ),
                null
        );

        SubmissionCore.SubmissionResult result = orchestrator.prepare(request);
        assertEquals("pending", result.status());
        assertTrue(result.message().contains("停车后"));
    }

    static void missingIdentityShouldReturnRequiredFields() {
        SubmissionCore.SubmissionOrchestrator orchestrator = SubmissionCore.buildRuntimeCore();

        SubmissionCore.SubmissionRequest request = new SubmissionCore.SubmissionRequest(
                "chengdu",
                false,
                new SubmissionCore.EvidencePayload(
                        "2026-01-01T08:00:00+08:00",
                        "成都市高新区XX路",
                        "川A12345",
                        "闯红灯/逆行",
                        "file://evidence.mp4"
                ),
                new SubmissionCore.IdentityProfile(null, null)
        );

        SubmissionCore.SubmissionResult result = orchestrator.prepare(request);
        assertEquals("need_user_input", result.status());
        assertEquals(Set.of("phone", "national_id"), Set.copyOf(result.missingFields()));
    }

    static void shenzhenRequestShouldBuildPayload() {
        SubmissionCore.SubmissionOrchestrator orchestrator = SubmissionCore.buildRuntimeCore();

        SubmissionCore.SubmissionRequest request = new SubmissionCore.SubmissionRequest(
                "shenzhen",
                false,
                new SubmissionCore.EvidencePayload(
                        "2026-01-01T08:00:00+08:00",
                        "深圳市福田区XX路",
                        "粤B54321",
                        "危险超车时变道不打灯",
                        "file://evidence.mp4"
                ),
                new SubmissionCore.IdentityProfile("13800000000", "440301199901010022")
        );

        SubmissionCore.SubmissionResult result = orchestrator.prepare(request);
        assertEquals("ready_for_confirmation", result.status());
        assertEquals("shenzhen", result.payload().get("city"));
        assertEquals("13800000000", result.payload().get("reporter_phone"));
        assertEquals("危险超车时变道不打灯", result.payload().get("violation_type"));
    }

    private static void assertEquals(Object expected, Object actual) {
        if ((expected == null && actual != null) || (expected != null && !expected.equals(actual))) {
            throw new AssertionError("Expected: " + expected + ", Actual: " + actual);
        }
    }

    private static void assertTrue(boolean condition) {
        if (!condition) {
            throw new AssertionError("Condition must be true, but was false.");
        }
    }
}
