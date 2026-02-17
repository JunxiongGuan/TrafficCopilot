package com.trafficcopilot.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SubmissionCore {

    public record EvidencePayload(
            String timestamp,
            String location,
            String plate,
            String violationType,
            String videoUri
    ) {}

    public record IdentityProfile(String phone, String nationalId) {}

    public record SubmissionRequest(
            String city,
            boolean isDriving,
            EvidencePayload evidence,
            IdentityProfile identity
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

    public interface CityRuleAdapter {
        String cityCode();
        List<String> requiredIdentityFields();
        default boolean allowSubmissionWhenDriving() {
            return false;
        }

        default Map<String, String> buildPayload(SubmissionRequest request) {
            Map<String, String> payload = new HashMap<>();
            payload.put("city", cityCode());
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

    public static final class ShenzhenRuleAdapter implements CityRuleAdapter {
        @Override
        public String cityCode() {
            return "shenzhen";
        }

        @Override
        public List<String> requiredIdentityFields() {
            return List.of("phone", "national_id");
        }
    }

    public static final class ChengduRuleAdapter implements CityRuleAdapter {
        @Override
        public String cityCode() {
            return "chengdu";
        }

        @Override
        public List<String> requiredIdentityFields() {
            return List.of("phone", "national_id");
        }
    }

    public static final class CityRuleRegistry {
        private final Map<String, CityRuleAdapter> adapters;

        public CityRuleRegistry(List<CityRuleAdapter> adapters) {
            Map<String, CityRuleAdapter> index = new HashMap<>();
            for (CityRuleAdapter adapter : adapters) {
                index.put(adapter.cityCode(), adapter);
            }
            this.adapters = index;
        }

        public CityRuleAdapter find(String cityCode) {
            if (cityCode == null) {
                return null;
            }
            return adapters.get(cityCode.trim().toLowerCase(Locale.ROOT));
        }
    }

    public static final class IdentitySecurityManager {
        public List<String> missingRequiredFields(CityRuleAdapter adapter, IdentityProfile identity) {
            List<String> missing = new ArrayList<>();
            for (String field : adapter.requiredIdentityFields()) {
                if ("phone".equals(field) && (identity == null || isBlank(identity.phone()))) {
                    missing.add(field);
                }
                if ("national_id".equals(field) && (identity == null || isBlank(identity.nationalId()))) {
                    missing.add(field);
                }
            }
            return missing;
        }
    }

    public static final class SubmissionOrchestrator {
        private final CityRuleRegistry cityRuleRegistry;
        private final IdentitySecurityManager identitySecurityManager;

        public SubmissionOrchestrator(
                CityRuleRegistry cityRuleRegistry,
                IdentitySecurityManager identitySecurityManager
        ) {
            this.cityRuleRegistry = cityRuleRegistry;
            this.identitySecurityManager = identitySecurityManager;
        }

        public SubmissionResult prepare(SubmissionRequest request) {
            CityRuleAdapter adapter = cityRuleRegistry.find(request.city());
            if (adapter == null) {
                return SubmissionResult.of(
                        "failed",
                        "City is not supported currently. Supported: shenzhen, chengdu."
                );
            }

            if (request.isDriving() && !adapter.allowSubmissionWhenDriving()) {
                return SubmissionResult.of(
                        "pending",
                        "检测到行车中状态，已保存取证，需停车后再确认提交。"
                );
            }

            List<String> missingFields = identitySecurityManager.missingRequiredFields(adapter, request.identity());
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
    }

    public static SubmissionOrchestrator buildRuntimeCore() {
        CityRuleRegistry cityRuleRegistry = new CityRuleRegistry(
                List.of(new ShenzhenRuleAdapter(), new ChengduRuleAdapter())
        );
        return new SubmissionOrchestrator(cityRuleRegistry, new IdentitySecurityManager());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
