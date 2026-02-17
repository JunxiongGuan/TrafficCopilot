from traffic_copilot.phase1 import (
    build_phase1_orchestrator,
    EvidencePayload,
    IdentityInfo,
    ReportRequest,
)


def test_phase1_only_supports_shenzhen_and_chengdu():
    orchestrator = build_phase1_orchestrator()

    request = ReportRequest(
        city="beijing",
        is_driving=False,
        evidence=EvidencePayload(
            timestamp="2026-01-01T08:00:00+08:00",
            location="北京市朝阳区XX路",
            plate="京A12345",
            violation_type="实线变道",
            video_uri="file://evidence.mp4",
        ),
        identity=IdentityInfo(phone="13800000000", national_id="510101199001010011"),
    )

    result = orchestrator.prepare_submission(request)

    assert result.status == "failed"
    assert "phase1" in result.message.lower()


def test_driving_mode_blocks_submission_and_creates_pending_case():
    orchestrator = build_phase1_orchestrator()

    request = ReportRequest(
        city="shenzhen",
        is_driving=True,
        evidence=EvidencePayload(
            timestamp="2026-01-01T08:00:00+08:00",
            location="深圳市南山区XX大道",
            plate="粤B12345",
            violation_type="占用应急车道",
            video_uri="file://evidence.mp4",
        ),
        identity=None,
    )

    result = orchestrator.prepare_submission(request)

    assert result.status == "pending"
    assert result.missing_fields == []
    assert "停车后" in result.message


def test_missing_identity_prompts_manual_input_when_required():
    orchestrator = build_phase1_orchestrator()

    request = ReportRequest(
        city="chengdu",
        is_driving=False,
        evidence=EvidencePayload(
            timestamp="2026-01-01T08:00:00+08:00",
            location="成都市高新区XX路",
            plate="川A12345",
            violation_type="闯红灯/逆行",
            video_uri="file://evidence.mp4",
        ),
        identity=IdentityInfo(phone=None, national_id=None),
    )

    result = orchestrator.prepare_submission(request)

    assert result.status == "need_user_input"
    assert set(result.missing_fields) == {"phone", "national_id"}


def test_successful_autofill_for_shenzhen():
    orchestrator = build_phase1_orchestrator()

    request = ReportRequest(
        city="shenzhen",
        is_driving=False,
        evidence=EvidencePayload(
            timestamp="2026-01-01T08:00:00+08:00",
            location="深圳市福田区XX路",
            plate="粤B54321",
            violation_type="危险超车时变道不打灯",
            video_uri="file://evidence.mp4",
        ),
        identity=IdentityInfo(phone="13800000000", national_id="440301199901010022"),
    )

    result = orchestrator.prepare_submission(request)

    assert result.status == "ready_for_confirmation"
    assert result.payload["city"] == "shenzhen"
    assert result.payload["reporter_phone"] == "13800000000"
    assert result.payload["violation_type"] == "危险超车时变道不打灯"
