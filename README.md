# TrafficCopilot (Android Phase 1 Core)

你这个判断是对的：做 **安卓 App**，主实现语言应使用 **Kotlin 或 Java**，而不是 Python。

为了兼容性和可落地性，本仓库当前已切到 **Java（Android 可直接使用）** 来实现 Phase 1 业务核心。

## 当前实现
- 语言：Java 17+
- 实现层：Phase 1 提交编排核心（可直接集成到 Android App）
- 城市范围：深圳、成都

## 核心能力
1. 仅支持 Phase 1 城市（深圳/成都）
2. 行车中阻断提交（仅保留取证，待停车后确认）
3. 身份字段缺失检测（phone / national_id）
4. 自动生成提交 payload，进入 `ready_for_confirmation`

## 目录
```text
.
├── docs/
│   └── traffic-violation-ai-assistant-scope.md
├── src/
│   ├── main/java/com/trafficcopilot/phase1/Phase1Orchestrator.java
│   └── test/java/com/trafficcopilot/phase1/Phase1OrchestratorTest.java
└── README.md
```

## 运行测试（无外部依赖）
```bash
mkdir -p out && \
javac -encoding UTF-8 -d out \
  src/main/java/com/trafficcopilot/phase1/Phase1Orchestrator.java \
  src/test/java/com/trafficcopilot/phase1/Phase1OrchestratorTest.java && \
java -cp out com.trafficcopilot.phase1.Phase1OrchestratorTest
```

## 下一步
- 把该核心层接入 Android App：Repository + ViewModel + 提交确认页 UI。
- Phase 1 测试通过后，再进入 Phase 2（保持阶段门禁）。
