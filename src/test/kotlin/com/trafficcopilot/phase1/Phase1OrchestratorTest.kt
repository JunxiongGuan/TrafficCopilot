package com.trafficcopilot.phase1

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Phase1OrchestratorTest {

    @Test
    fun phase1OnlySupportsShenzhenAndChengdu() {
        val orchestrator = buildPhase1Orchestrator()
        val request = ReportRequest(
            city = "beijing",
            isDriving = false,
            evidence = EvidencePayload(
                timestamp = "2026-01-01T08:00:00+08:00",
                location = "北京市朝阳区XX路",
                plate = "京A12345",
                violationType = "实线变道",
                videoUri = "file://evidence.mp4",
            ),
            identity = IdentityInfo(phone = "13800000000", nationalId = "510101199001010011"),
        )

        val result = orchestrator.prepareSubmission(request)

        assertEquals("failed", result.status)
        assertTrue(result.message.contains("Phase1"))
    }

    @Test
    fun drivingModeBlocksSubmissionAndCreatesPendingCase() {
        val orchestrator = buildPhase1Orchestrator()
        val request = ReportRequest(
            city = "shenzhen",
            isDriving = true,
            evidence = EvidencePayload(
                timestamp = "2026-01-01T08:00:00+08:00",
                location = "深圳市南山区XX大道",
                plate = "粤B12345",
                violationType = "占用应急车道",
                videoUri = "file://evidence.mp4",
            ),
            identity = null,
        )

        val result = orchestrator.prepareSubmission(request)

        assertEquals("pending", result.status)
        assertTrue(result.missingFields.isEmpty())
        assertTrue(result.message.contains("停车后"))
    }

    @Test
    fun missingIdentityPromptsManualInputWhenRequired() {
        val orchestrator = buildPhase1Orchestrator()
        val request = ReportRequest(
            city = "chengdu",
            isDriving = false,
            evidence = EvidencePayload(
                timestamp = "2026-01-01T08:00:00+08:00",
                location = "成都市高新区XX路",
                plate = "川A12345",
                violationType = "闯红灯/逆行",
                videoUri = "file://evidence.mp4",
            ),
            identity = IdentityInfo(phone = null, nationalId = null),
        )

        val result = orchestrator.prepareSubmission(request)

        assertEquals("need_user_input", result.status)
        assertEquals(setOf("phone", "national_id"), result.missingFields.toSet())
    }

    @Test
    fun successfulAutofillForShenzhen() {
        val orchestrator = buildPhase1Orchestrator()
        val request = ReportRequest(
            city = "shenzhen",
            isDriving = false,
            evidence = EvidencePayload(
                timestamp = "2026-01-01T08:00:00+08:00",
                location = "深圳市福田区XX路",
                plate = "粤B54321",
                violationType = "危险超车时变道不打灯",
                videoUri = "file://evidence.mp4",
            ),
            identity = IdentityInfo(phone = "13800000000", nationalId = "440301199901010022"),
        )

        val result = orchestrator.prepareSubmission(request)

        assertEquals("ready_for_confirmation", result.status)
        assertEquals("shenzhen", result.payload["city"])
        assertEquals("13800000000", result.payload["reporter_phone"])
        assertEquals("危险超车时变道不打灯", result.payload["violation_type"])
    }
}
