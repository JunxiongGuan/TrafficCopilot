from dataclasses import dataclass, field
from typing import Dict, List, Optional


SUPPORTED_PHASE1_CITIES = {"shenzhen", "chengdu"}


@dataclass
class EvidencePayload:
    timestamp: str
    location: str
    plate: str
    violation_type: str
    video_uri: str


@dataclass
class IdentityInfo:
    phone: Optional[str]
    national_id: Optional[str]


@dataclass
class ReportRequest:
    city: str
    is_driving: bool
    evidence: EvidencePayload
    identity: Optional[IdentityInfo]


@dataclass
class SubmissionResult:
    status: str
    message: str
    payload: Dict[str, str] = field(default_factory=dict)
    missing_fields: List[str] = field(default_factory=list)


class CityAdapter:
    def __init__(self, city: str, required_identity_fields: List[str]):
        self.city = city
        self.required_identity_fields = required_identity_fields

    def build_payload(self, request: ReportRequest) -> Dict[str, str]:
        payload = {
            "city": self.city,
            "violation_type": request.evidence.violation_type,
            "occurred_at": request.evidence.timestamp,
            "occurred_location": request.evidence.location,
            "target_plate": request.evidence.plate,
            "evidence_video": request.evidence.video_uri,
        }

        if request.identity:
            if request.identity.phone:
                payload["reporter_phone"] = request.identity.phone
            if request.identity.national_id:
                payload["reporter_national_id"] = request.identity.national_id

        return payload


class SubmissionOrchestrator:
    def __init__(self, adapters: Dict[str, CityAdapter]):
        self.adapters = adapters

    def prepare_submission(self, request: ReportRequest) -> SubmissionResult:
        city = request.city.lower().strip()
        adapter = self.adapters.get(city)

        if not adapter:
            return SubmissionResult(
                status="failed",
                message="City is not supported in Phase1. Supported: shenzhen, chengdu.",
            )

        if request.is_driving:
            return SubmissionResult(
                status="pending",
                message="检测到行车中状态，已保存取证，需停车后再确认提交。",
            )

        missing_fields = self._missing_identity_fields(adapter, request.identity)
        if missing_fields:
            return SubmissionResult(
                status="need_user_input",
                message="提交该城市平台前需补全身份信息。",
                missing_fields=missing_fields,
            )

        payload = adapter.build_payload(request)
        return SubmissionResult(
            status="ready_for_confirmation",
            message="材料已按城市模板自动填报，请用户确认最终结果。",
            payload=payload,
        )

    @staticmethod
    def _missing_identity_fields(
        adapter: CityAdapter, identity: Optional[IdentityInfo]
    ) -> List[str]:
        if not identity:
            return list(adapter.required_identity_fields)

        missing: List[str] = []
        for field_name in adapter.required_identity_fields:
            if not getattr(identity, field_name):
                missing.append(field_name)
        return missing


def build_phase1_orchestrator() -> SubmissionOrchestrator:
    adapters = {
        "shenzhen": CityAdapter(
            city="shenzhen", required_identity_fields=["phone", "national_id"]
        ),
        "chengdu": CityAdapter(
            city="chengdu", required_identity_fields=["phone", "national_id"]
        ),
    }
    return SubmissionOrchestrator(adapters)
