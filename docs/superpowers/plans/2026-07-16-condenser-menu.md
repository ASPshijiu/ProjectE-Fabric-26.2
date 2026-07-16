# 凝聚器生存模式菜单实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 让生存模式玩家可打开 MK1/MK2 凝聚器，设置或清除目标物品，并通过与 ProjectE 1.21.1 一致的槽位布局存取物品。

**架构：** 使用一个按等级参数化的 `CondenserMenu` 承载两种布局，真实库存由服务端方块实体持有，客户端使用同尺寸临时容器接收原版槽位同步。目标物品使用不消耗手持物的幽灵槽，EMC 余额与目标需求通过两个 `SyncedLong` 同步；客户端屏幕直接复用仓库已有的上游纹理。

**技术栈：** Java 25、Minecraft 26.2 Mojang mappings、Fabric Loader 0.19.3、Fabric API 0.154.2、JUnit 5、Gradle 9.5.1。

---

### 任务 1：锁定菜单协议与槽位语义

**文件：**
- 创建：`src/test/java/moze_intel/projecte/content/menu/CondenserMenuTest.java`
- 修改：`src/test/java/moze_intel/projecte/content/ModMenuTypesTest.java`

- [ ] **步骤 1：编写失败的菜单测试**

测试必须断言：MK1 为 `1 + 91 + 36` 个菜单槽；MK2 为 `1 + 42 + 42 + 36` 个菜单槽；MK2 输出槽拒绝放入；点击幽灵槽设置单个目标副本且不消耗 carried stack；再次点击清除目标；进度条在无目标、半进度和完成时分别为 0、51、102。

```java
assertEquals(128, mk1.slots.size());
assertEquals(121, mk2.slots.size());
assertFalse(mk2.getSlot(43).mayPlace(new ItemStack(Items.DIAMOND)));
assertEquals(51, CondenserMenu.progressScaled(4_096, 8_192));
```

- [ ] **步骤 2：运行测试并确认红灯**

运行：

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home sh ./gradlew test --tests moze_intel.projecte.content.menu.CondenserMenuTest --tests moze_intel.projecte.content.ModMenuTypesTest
```

预期：测试编译失败，因为 `CondenserMenu` 和两个凝聚器菜单标识符尚不存在。

### 任务 2：实现服务端菜单与目标交互

**文件：**
- 创建：`src/main/java/moze_intel/projecte/content/menu/CondenserMenu.java`
- 修改：`src/main/java/moze_intel/projecte/content/blocks/CondenserBlockEntity.java`

- [ ] **步骤 1：实现两级槽位布局**

`CondenserMenu` 的菜单索引固定为：0 是目标幽灵槽；MK1 的 1-91 是共享库存；MK2 的 1-42 是输入、43-84 是只读输出；剩余 36 格是玩家背包和快捷栏。玩家 shift-click 只把有正 EMC 且不等于目标的物品送入输入范围。

```java
int firstPlayerSlot = 1 + machineSlots;
return MenuQuickMove.move(slot, stack -> index < firstPlayerSlot
      ? moveItemStackTo(stack, firstPlayerSlot, slots.size(), false)
      : moveItemStackTo(stack, 1, 1 + inputSlots, false));
```

- [ ] **步骤 2：实现幽灵目标与 long 数据同步**

目标槽覆盖 `mayPlace`、`mayPickup` 和 `isFake`，不直接改动容器。服务端收到目标槽点击后验证 carried stack 的 EMC，调用 `setTarget`/`refreshTargetEmc`，并刷新同步用的 `SimpleContainer`；再次点击有效目标时清空目标。使用两个 `SyncedLong` 分别同步 stored EMC 和 required EMC。

```java
if (slotId == TARGET_SLOT && condenser != null) {
    updateTargetFromCarried(getCarried());
    refreshTargetSlot();
    broadcastChanges();
    return;
}
```

- [ ] **步骤 3：运行菜单测试并确认绿灯**

运行任务 1 的测试命令，预期全部通过。

### 任务 3：接通方块、注册表和客户端界面

**文件：**
- 修改：`src/main/java/moze_intel/projecte/content/ModMenuTypes.java`
- 修改：`src/main/java/moze_intel/projecte/content/blocks/CondenserBlock.java`
- 创建：`src/client/java/moze_intel/projecte/client/screen/CondenserScreen.java`
- 修改：`src/client/java/moze_intel/projecte/client/ProjectEClient.java`

- [ ] **步骤 1：注册两种菜单类型并允许右键打开**

注册 `projecte:condenser_mk1` 和 `projecte:condenser_mk2`，客户端工厂分别创建对应等级的临时菜单。`CondenserBlock.useWithoutItem` 仅在服务端调用 `player.openMenu(condenser)`，双方返回 `InteractionResult.SUCCESS`。

- [ ] **步骤 2：实现纹理屏幕**

屏幕尺寸固定为 255x233，MK1 使用 `textures/gui/condenser.png`，MK2 使用 `textures/gui/condenser_mk2.png`。背景绘制完整面板，进度条从纹理 `(0, 235)` 截取宽度 0-102，并在 `(140, 10)` 显示 `min(stored, required)` EMC。

```java
extractor.blit(RenderPipelines.GUI_TEXTURED, texture,
      leftPos + 33, topPos + 10, 0.0f, 235.0f,
      menu.progressScaled(), 10, 256, 256);
```

- [ ] **步骤 3：编译客户端并运行定向测试**

运行：

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home sh ./gradlew compileClientJava test --tests moze_intel.projecte.content.menu.CondenserMenuTest --tests moze_intel.projecte.content.blocks.CondenserBlockEntityTest
```

预期：`BUILD SUCCESSFUL`。

### 任务 4：完成验证、提交和推送

**文件：**
- 验证上述全部文件与现有测试套件。

- [ ] **步骤 1：运行完整构建与打包审计**

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home sh ./gradlew clean build packageAudit
```

预期：全部测试 0 failures、0 errors，`packageAudit` 成功。

- [ ] **步骤 2：运行 Minecraft 26.2 服务端验证**

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home sh ./gradlew runServer
```

预期：Fabric Loader 0.19.3 加载 ProjectE，服务端进入 `Done`，发送一次 `stop` 后 `BUILD SUCCESSFUL`。

- [ ] **步骤 3：签名提交并推送**

```bash
git add docs/superpowers/plans/2026-07-16-condenser-menu.md src/main src/client src/test
git commit -S -m "修复：恢复凝聚器生存模式菜单"
git push origin port/emc-core
```

推送后通过 GitHub API 确认提交为 `verified=true`、`reason=valid`，并确认工作树干净、本地与远端差异为 `0/0`。
