# ProjectE Fabric 26.2 完整移植路线图

日期：2026-07-12  
权威规格：../specs/2026-07-12-projecte-fabric-26.2-design.md

## 目标

以 ProjectE 1.21.1 提交 15d4ce65bd06eb4222709b984255fbf5080e78bc 为最低功能基线，在 Fabric 26.2 上完成无阉割移植，并吸收其他等价交换实现中不冲突的新增能力。

## 计划拆分原则

该工程包含 EMC 图算法、玩家持久化、网络协议、转化 GUI、自动化机器、范围工具、装备能力、数据生成和第三方兼容等多个可独立审查的子系统。每个阶段都必须生成单独实施计划、通过测试并提交，之后才能进入下一阶段。拆分仅用于控制风险，不缩减最终范围。

## 阶段与交付物

| 阶段 | 计划文件 | 独立交付物 | 完成门禁 |
| --- | --- | --- | --- |
| 1 | 2026-07-12-projecte-fabric-26.2-foundation.md | 可构建的 26.2 Fabric 工程、许可证、CI、上游基线清单、EMC 数值原语 | build、单元测试、基线导出确定性检查全部通过 |
| 2 | 2026-07-12-projecte-fabric-26.2-emc-core.md | 归一化堆栈、显式 EMC、配方推导、循环/溢出处理、数据包重载 | ProjectE EMC 快照与算法测试通过 |
| 3 | 2026-07-12-projecte-fabric-26.2-player-data-network.md | 玩家 EMC/知识、Codec、命令、服务端权威网络同步 | 重连、重启、非法包和迁移测试通过 |
| 4 | 2026-07-12-projecte-fabric-26.2-transmutation.md | 贤者之石、世界转化、转化桌、便携桌、知识宝典 | 单人及双人并发转化流程通过 |
| 5 | 2026-07-12-projecte-fabric-26.2-machines-storage.md | 收集器、中继器、凝聚器、熔炉、炼金箱/袋、自动化 | GameTest、吞吐和复制防护测试通过 |
| 6 | 2026-07-12-projecte-fabric-26.2-tools-equipment.md | 暗/红物质工具盔甲、宝石套装、戒指、护符、台座与投射物 | 每个能力的主动/被动/存档测试通过 |
| 7 | 2026-07-12-projecte-fabric-26.2-content-datagen.md | 全部配方、进度、战利品、标签、模型、语言、声音与纹理 | 标识符与语义快照无未解释差异 |
| 8 | 2026-07-12-projecte-fabric-26.2-compat-migration.md | EMI、REI、Jade/WTHIT、Trinkets、Transfer API、NeoForge 数据导入 | 有/无可选依赖均可启动，导入测试通过 |
| 9 | 2026-07-12-projecte-fabric-26.2-final-verification.md | 客户端、专服、多人、生存流程和功能矩阵最终审计 | 所有基线项为已验证，无空实现和未解释差异 |

## 全局不可变约束

- Minecraft 26.2、Java 25、Loom 1.17、Gradle 9.5.1。
- Fabric Loader 0.19.3，Fabric API 0.154.2+26.2；升级必须经过完整回归。
- 模组 ID 保持 projecte，公开包名保持 moze_intel.projecte。
- ProjectE MIT 许可证和版权声明必须保留。
- 服务端对 EMC、知识、机器、交易和世界操作拥有最终权威。
- 可选兼容不得成为启动硬依赖。
- 任何上游注册物、能力或数据文件只有在功能矩阵标记为已验证后才算完成。
- 不接受空方法、永久默认返回值、只注册不实现或只通过启动测试的降级。

## 阶段推进规则

1. 每阶段开始前写出对应详细实施计划。
2. 每个任务使用测试先行并形成独立提交。
3. 阶段结束时更新 docs/porting/feature-matrix.md 和证据链接。
4. 当前阶段所有门禁通过后才开始下一阶段。
5. 新发现的上游功能必须加入矩阵和相应阶段，不得静默忽略。
