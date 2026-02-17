package com.trafficcopilot.phase1;

import java.util.Set;

public class Phase1OrchestratorTest {

    public static void main(String[] args) {
        testPhase1OnlySupportsShenzhenAndChengdu();
        testDrivingModeBlocksSubmissionAndCreatesPendingCase();
        testMissingIdentityPromptsManualInputWhenRequired();
        testSuccessfulAutofillForShenzhen();
        System.out.println("All Phase1Orchestrator tests passed.");
    }

    static void testPhase1OnlySupportsShenzhenAndChengdu() {
        Phase1Orchestrator orchestrator = Phase1Orchestrator.buildDefault();
        Phase1Orchestrator.ReportRequest request = new Phase1Orchestrator.ReportRequest(
                "beijing",
                false,
                new Phase1Orchestrator.EvidencePayload(
                        "2026-01-01T08:00:00+08:00",
                        "北京市朝阳区XX路",
                        "京A12345",
                        "实线变道",
                        "file://evidence.mp4"
                ),
                new Phase1Orchestrator.IdentityInfo("13800000000", "510101199001010011")
        );

        Phase1Orchestrator.SubmissionResult result = orchestrator.prepareSubmission(request);
        assertEquals("failed", result.status());
        assertTrue(result.message().contains("Phase1"));
    }

    static void testDrivingModeBlocksSubmissionAndCreatesPendingCase() {
        Phase1Orchestrator orchestrator = Phase1Orchestrator.buildDefault();
        Phase1Orchestrator.ReportRequest request = new Phase1Orchestrator.ReportRequest(
                "shenzhen",
                true,
                new Phase1Orchestrator.EvidencePayload(
                        "2026-01-01T08:00:00+08:00",
                        "深圳市南山区XX大道",
                        "粤B12345",
                        "占用应急车道",
                        "file://evidence.mp4"
                ),
                null
        );

        Phase1Orchestrator.SubmissionResult result = orchestrator.prepareSubmission(request);
        assertEquals("pending", result.status());
        assertTrue(result.missingFields().isEmpty());
        assertTrue(result.message().contains("停车后"));
    }

    static void testMissingIdentityPromptsManualInputWhenRequired() {
        Phase1Orchestrator orchestrator = Phase1Orchestrator.buildDefault();
        Phase1Orchestrator.ReportRequest request = new Phase1Orchestrator.ReportRequest(
                "chengdu",
                false,
                new Phase1Orchestrator.EvidencePayload(
                        "2026-01-01T08:00:00+08:00",
                        "成都市高新区XX路",
                        "川A12345",
                        "闯红灯/逆行",
                        "file://evidence.mp4"
                ),
                new Phase1Orchestrator.IdentityInfo(null, null)
        );

        Phase1Orchestrator.SubmissionResult result = orchestrator.prepareSubmission(request);
        assertEquals("need_user_input", result.status());
        assertEquals(Set.of("phone", "national_id"), Set.copyOf(result.missingFields()));
    }

    static void testSuccessfulAutofillForShenzhen() {
        Phase1Orchestrator orchestrator = Phase1Orchestrator.buildDefault();
        Phase1Orchestrator.ReportRequest request = new Phase1Orchestrator.ReportRequest(
                "shenzhen",
                false,
                new Phase1Orchestrator.EvidencePayload(
                        "2026-01-01T08:00:00+08:00",
                        "深圳市福田区XX路",
                        "粤B54321",
                        "危险超车时变道不打灯",
                        "file://evidence.mp4"
                ),
                new Phase1Orchestrator.IdentityInfo("13800000000", "440301199901010022")
        );

        Phase1Orchestrator.SubmissionResult result = orchestrator.prepareSubmission(request);
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
