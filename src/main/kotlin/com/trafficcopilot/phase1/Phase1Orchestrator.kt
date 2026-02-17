package com.trafficcopilot.phase1

data class EvidencePayload(
    val timestamp: String,
    val location: String,
    val plate: String,
    val violationType: String,
    val videoUri: String,
)

data class IdentityInfo(
    val phone: String?,
    val nationalId: String?,
)

data class ReportRequest(
    val city: String,
    val isDriving: Boolean,
    val evidence: EvidencePayload,
    val identity: IdentityInfo?,
)

data class SubmissionResult(
    val status: String,
    val message: String,
    val payload: Map<String, String> = emptyMap(),
    val missingFields: List<String> = emptyList(),
)

class CityAdapter(
    private val city: String,
    private val requiredIdentityFields: List<String>,
) {
    fun buildPayload(request: ReportRequest): Map<String, String> {
        val payload = mutableMapOf(
            "city" to city,
            "violation_type" to request.evidence.violationType,
            "occurred_at" to request.evidence.timestamp,
            "occurred_location" to request.evidence.location,
            "target_plate" to request.evidence.plate,
            "evidence_video" to request.evidence.videoUri,
        )

        request.identity?.phone?.let { payload["reporter_phone"] = it }
        request.identity?.nationalId?.let { payload["reporter_national_id"] = it }

        return payload
    }

    fun missingIdentityFields(identity: IdentityInfo?): List<String> {
        if (identity == null) {
            return requiredIdentityFields
        }
        return requiredIdentityFields.filter { fieldName ->
            when (fieldName) {
                "phone" -> identity.phone.isNullOrBlank()
                "national_id" -> identity.nationalId.isNullOrBlank()
                else -> true
            }
        }
    }
}

class SubmissionOrchestrator(private val adapters: Map<String, CityAdapter>) {

    fun prepareSubmission(request: ReportRequest): SubmissionResult {
        val city = request.city.trim().lowercase()
        val adapter = adapters[city]
            ?: return SubmissionResult(
                status = "failed",
                message = "City is not supported in Phase1. Supported: shenzhen, chengdu.",
            )

        if (request.isDriving) {
            return SubmissionResult(
                status = "pending",
                message = "检测到行车中状态，已保存取证，需停车后再确认提交。",
            )
        }

        val missing = adapter.missingIdentityFields(request.identity)
        if (missing.isNotEmpty()) {
            return SubmissionResult(
                status = "need_user_input",
                message = "提交该城市平台前需补全身份信息。",
                missingFields = missing,
            )
        }

        return SubmissionResult(
            status = "ready_for_confirmation",
            message = "材料已按城市模板自动填报，请用户确认最终结果。",
            payload = adapter.buildPayload(request),
        )
    }
}

fun buildPhase1Orchestrator(): SubmissionOrchestrator {
    val adapters = mapOf(
        "shenzhen" to CityAdapter("shenzhen", listOf("phone", "national_id")),
        "chengdu" to CityAdapter("chengdu", listOf("phone", "national_id")),
    )
    return SubmissionOrchestrator(adapters)
}
