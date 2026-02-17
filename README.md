# TrafficCopilot

交通违法举报个人 AI 助手（Phase 1 原型）。

本仓库当前实现了一个可测试的 **Phase 1 提交编排核心**，面向“行中取证、停车后确认提交”的合规流程，首批仅支持：

- 深圳（shenzhen）
- 成都（chengdu）

---

## 当前能力（Phase 1）

`src/traffic_copilot/phase1.py` 提供以下核心能力：

1. **城市范围控制（Phase 1）**
   - 仅允许深圳/成都进入自动编排流程。
   - 非 Phase 1 城市会返回失败状态与提示信息。

2. **行车中阻断提交**
   - 检测到 `is_driving=True` 时，不允许进入提交。
   - 返回 `pending` 状态，提示停车后再确认。

3. **身份信息缺失校验**
   - 按城市模板要求校验身份字段（当前为 `phone` / `national_id`）。
   - 缺失时返回 `need_user_input` 和缺失字段列表。

4. **自动填报载荷生成**
   - 在满足提交前置条件后，返回 `ready_for_confirmation`。
   - 输出标准化 payload，供“最终确认页”使用。

---

## 项目结构

```text
.
├── docs/
│   └── traffic-violation-ai-assistant-scope.md
├── src/
│   └── traffic_copilot/
│       ├── __init__.py
│       └── phase1.py
├── tests/
│   └── test_phase1_submission.py
├── pyproject.toml
└── README.md
```

---

## 安装与运行

### 1) 准备环境

- Python 3.10+

### 2) 安装测试依赖（如未安装 pytest）

```bash
pip install pytest
```

### 3) 运行测试

```bash
pytest
```

当前测试覆盖：

- 非 Phase 1 城市拦截
- 行车中状态转 pending
- 身份信息缺失提示
- 深圳自动填报成功路径

---

## 使用示例

```python
from traffic_copilot.phase1 import (
    build_phase1_orchestrator,
    EvidencePayload,
    IdentityInfo,
    ReportRequest,
)

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
    identity=IdentityInfo(
        phone="13800000000",
        national_id="440301199901010022",
    ),
)

result = orchestrator.prepare_submission(request)
print(result.status)
print(result.payload)
```

---

## 后续计划

- 按文档中的阶段门禁推进：
  - 先完成 Phase 1（深圳 + 成都）闭环与测试验证
  - Phase 1 验收通过后，再进入 Phase 2

详见：`docs/traffic-violation-ai-assistant-scope.md`
