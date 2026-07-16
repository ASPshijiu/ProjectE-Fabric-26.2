# 凝聚器 Fabric Transfer API 自动化实现计划

> **面向当前主代理：** 按用户要求内联执行，不使用子代理。步骤使用复选框（`- [ ]`）跟踪，并保持一个 bug 一个签名提交、每次提交立即推送。

**目标：** 为 MK1/MK2 凝聚器恢复与 ProjectE 1.21.1 一致的物品自动化，并保留 Fabric Transfer API 的事务回滚语义。

**架构：** 新增专用的 `CondenserItemStorage`，用 `ContainerStorage` 包装现有方块实体库存，再按等级组合 `FilteringStorage` 和 `CombinedStorage`。所有方向注册同一个规则；EMC 过滤读取当前服务端映射，测试通过可注入谓词隔离验证槽位和事务行为。

**技术栈：** Java 25、Minecraft 26.2、Fabric Loader 0.19.3、Fabric Transfer API 8.0.11、JUnit 5、Gradle 9.5.1。

---

### 任务 1：锁定自动化行为

**文件：**
- 创建：`src/test/java/moze_intel/projecte/content/blocks/CondenserItemStorageTest.java`

- [ ] **步骤 1：编写 MK1 失败测试**

测试必须断言：正 EMC 的非目标物品可插入；目标和无 EMC 物品被拒绝；共享库存只允许提取有效目标。

```java
assertEquals(4, storage.insert(redstone, 4, transaction));
assertEquals(0, storage.insert(diamondTarget, 1, transaction));
assertEquals(0, storage.extract(redstone, 1, transaction));
assertEquals(2, storage.extract(diamondTarget, 2, transaction));
```

- [ ] **步骤 2：编写 MK2 与事务失败测试**

测试必须断言：插入只进入 `0-41`，提取只来自 `42-83`；关闭未提交事务后，插入和提取都回滚。

- [ ] **步骤 3：运行测试并确认红灯**

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home sh ./gradlew test --tests moze_intel.projecte.content.blocks.CondenserItemStorageTest
```

预期：测试编译失败，因为 `CondenserItemStorage` 尚不存在。

### 任务 2：实现分级 Transfer API 存储

**文件：**
- 创建：`src/main/java/moze_intel/projecte/content/blocks/CondenserItemStorage.java`

- [ ] **步骤 1：包装 MK1 库存**

使用 `ContainerStorage.of(condenser, null)` 包装 91 格共享库存。插入过滤器要求物品有正 EMC 且不匹配目标；提取过滤器要求资源匹配 EMC 有效的目标。

- [ ] **步骤 2：组合 MK2 输入输出**

用前 42 个 `SingleSlotStorage` 构造仅插入的输入存储，用后 42 个构造仅提取的输出存储，再通过 `CombinedStorage` 合并。

- [ ] **步骤 3：运行定向测试并确认绿灯**

运行任务 1 的测试命令，预期全部通过且无错误。

### 任务 3：注册方块实体存储接口

**文件：**
- 修改：`src/main/java/moze_intel/projecte/ProjectE.java`
- 测试：`src/test/java/moze_intel/projecte/content/blocks/CondenserItemStorageTest.java`

- [ ] **步骤 1：注册 MK1/MK2**

在 `ModBlocks.init()` 完成方块实体类型创建后调用 `CondenserItemStorage.init()`，通过 `ItemStorage.SIDED.registerForBlockEntity(...)` 注册两个等级，方向参数不参与规则选择。

- [ ] **步骤 2：编译并运行凝聚器相关测试**

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home sh ./gradlew test --tests 'moze_intel.projecte.content.blocks.Condenser*Test'
```

预期：全部通过。

### 任务 4：完整验证、签名提交和推送

**文件：**
- 验证本计划涉及的全部生产与测试文件。

- [ ] **步骤 1：运行完整构建与打包审计**

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home sh ./gradlew clean build packageAudit
```

预期：全部测试 0 failures、0 errors，`packageAudit` 成功。

- [ ] **步骤 2：运行 Minecraft 26.2 服务端验证**

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home sh ./gradlew runServer
```

预期：Fabric Loader 0.19.3 加载 ProjectE，服务端进入 `Done`；发送一次 `stop` 后 `BUILD SUCCESSFUL`。

- [ ] **步骤 3：签名提交、推送并核验**

```bash
git add docs/superpowers/plans/2026-07-16-condenser-transfer-api.md src/main/java/moze_intel/projecte/ProjectE.java src/main/java/moze_intel/projecte/content/blocks/CondenserItemStorage.java src/test/java/moze_intel/projecte/content/blocks/CondenserItemStorageTest.java
git commit -S -m "修复：恢复凝聚器物品自动化"
git push origin port/emc-core
```

推送后必须确认 GitHub `verified=true`、`reason=valid`，工作树干净，本地与远端差异为 `0/0`。
