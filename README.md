# TrafficCopilot Android Runtime Core

本仓库当前提供的是**安卓运行时核心编排层**（Java），按“核心能力技术架构”拆分实现，而不是按 `phase1` 命名业务代码。

## 架构模块
实现位于：`src/main/java/com/trafficcopilot/core/SubmissionCore.java`

- `CityRuleAdapter`：城市规则适配接口（字段要求、行车中提交约束、payload构建）
- `CityRuleRegistry`：城市适配器注册与查找
- `IdentitySecurityManager`：身份字段缺失检测
- `SubmissionOrchestrator`：提交编排（城市校验、行车态阻断、缺失项提示、自动填报）
- `buildRuntimeCore()`：运行时装配入口

> 当前默认内置城市适配器：深圳、成都。

## 测试
测试位于：`src/test/java/com/trafficcopilot/core/SubmissionCoreTest.java`

覆盖点：
1. 不支持城市时返回 `failed`
2. 行车中状态返回 `pending`
3. 身份信息缺失返回 `need_user_input` + 缺失字段
4. 深圳场景自动填报返回 `ready_for_confirmation`

运行：
```bash
mkdir -p out && \
javac -encoding UTF-8 -d out \
  src/main/java/com/trafficcopilot/core/SubmissionCore.java \
  src/test/java/com/trafficcopilot/core/SubmissionCoreTest.java && \
java -cp out com.trafficcopilot.core.SubmissionCoreTest
```

## 说明
- 产品路线文档仍保留阶段规划描述，见 `docs/traffic-violation-ai-assistant-scope.md`。
- 运行时代码采用架构能力命名，便于持续扩展更多城市适配器与提交流程。
