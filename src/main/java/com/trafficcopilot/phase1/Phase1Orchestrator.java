package com.trafficcopilot.phase1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Phase1Orchestrator {

    public record EvidencePayload(
            String timestamp,
            String location,
            String plate,
            String violationType,
            String videoUri
    ) {}

    public record IdentityInfo(String phone, String nationalId) {}

    public record ReportRequest(
            String city,
            boolean isDriving,
            EvidencePayload evidence,
            IdentityInfo identity
    ) {}

    public record SubmissionResult(
            String status,
            String message,
            Map<String, String> payload,
            List<String> missingFields
    ) {
        public static SubmissionResult of(String status, String message) {
            return new SubmissionResult(status, message, Map.of(), List.of());
        }
    }

    public static class CityAdapter {
        private final String city;
        private final List<String> requiredIdentityFields;

        public CityAdapter(String city, List<String> requiredIdentityFields) {
            this.city = city;
            this.requiredIdentityFields = requiredIdentityFields;
        }

        public List<String> missingIdentityFields(IdentityInfo identity) {
            if (identity == null) {
                return requiredIdentityFields;
            }

            List<String> missing = new ArrayList<>();
            for (String fieldName : requiredIdentityFields) {
                if ("phone".equals(fieldName) && isBlank(identity.phone())) {
                    missing.add(fieldName);
                }
                if ("national_id".equals(fieldName) && isBlank(identity.nationalId())) {
                    missing.add(fieldName);
                }
            }
            return missing;
        }

        public Map<String, String> buildPayload(ReportRequest request) {
            Map<String, String> payload = new HashMap<>();
            payload.put("city", city);
            payload.put("violation_type", request.evidence().violationType());
            payload.put("occurred_at", request.evidence().timestamp());
            payload.put("occurred_location", request.evidence().location());
            payload.put("target_plate", request.evidence().plate());
            payload.put("evidence_video", request.evidence().videoUri());

            if (request.identity() != null) {
                if (!isBlank(request.identity().phone())) {
                    payload.put("reporter_phone", request.identity().phone());
                }
                if (!isBlank(request.identity().nationalId())) {
                    payload.put("reporter_national_id", request.identity().nationalId());
                }
            }
            return payload;
        }
    }

    private final Map<String, CityAdapter> adapters;

    public Phase1Orchestrator(Map<String, CityAdapter> adapters) {
        this.adapters = adapters;
    }

    public SubmissionResult prepareSubmission(ReportRequest request) {
        String city = request.city().trim().toLowerCase(Locale.ROOT);
        CityAdapter adapter = adapters.get(city);
        if (adapter == null) {
            return SubmissionResult.of(
                    "failed",
                    "City is not supported in Phase1. Supported: shenzhen, chengdu."
            );
        }

        if (request.isDriving()) {
            return SubmissionResult.of(
                    "pending",
                    "检测到行车中状态，已保存取证，需停车后再确认提交。"
            );
        }

        List<String> missingFields = adapter.missingIdentityFields(request.identity());
        if (!missingFields.isEmpty()) {
            return new SubmissionResult(
                    "need_user_input",
                    "提交该城市平台前需补全身份信息。",
                    Map.of(),
                    missingFields
            );
        }

        return new SubmissionResult(
                "ready_for_confirmation",
                "材料已按城市模板自动填报，请用户确认最终结果。",
                adapter.buildPayload(request),
                List.of()
        );
    }

    public static Phase1Orchestrator buildDefault() {
        Map<String, CityAdapter> adapters = Map.of(
                "shenzhen", new CityAdapter("shenzhen", List.of("phone", "national_id")),
                "chengdu", new CityAdapter("chengdu", List.of("phone", "national_id"))
        );
        return new Phase1Orchestrator(adapters);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
