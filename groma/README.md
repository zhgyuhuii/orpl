# Groma Simulation Workspace 开发与门禁约定

## 1. 架构分层与包名隔离约定
- `com.groma.base.*`：公共底座（单写者引擎、WebSocket 背压总线、外部连接器、MCP 协议基础设施）。
- `com.groma.models.{modelcode}.*`：由方圆模型编译产出的独立五层业务模块，子模块间严禁互相依赖。
- `com.groma.bootstrap.*`：统一 Spring Boot 4.1 运行宿主（内嵌 Tomcat，装配 `FullyQualifiedAnnotationBeanNameGenerator`）。
- `com.groma.studio.*`：模型拉取、解析、命名转译、源码生成、增量同步、ECJ 热编译与 DAP 调试套件。

## 2. 编码硬性红线（每次提交由 `scan.py` 自动扫描）
1. 日志统一使用 SLF4J，严禁标准输出或打印异常栈。
2. 推演内核与连接器采用单写者事件循环 + 无锁队列或显式 `ReentrantLock`，严禁使用原生方法/块监视器锁。
3. 所有洗牌与概率分布采样必须从任务级 `seed` 派生 `SeededSampler`。
4. 配置文件外置为环境变量 `${GROMA_*:default}`，严禁出现本机路径、内网地址或明文口令。

## 3. 快速构建与门禁验证

```bash
# 1. 验证存量六个目录零修改（在仓库根目录执行）
bash reliability-spec/scripts/run-check.sh CMD-NP-00

# 2. 全量多模型编译与测试
mvn -B clean package

# 3. 单模型裁剪独立打包（仅打包应急决策推演模型）
mvn -B clean package -Psingle-model -Dtarget.model=model-emergency-deduction

# 4. 前后端红线静态自检（本工程自带门禁）
python3 -I tools/regression/scan.py all .

# 5. 一键验收总控（缺失任一必交文档返回退出码 2）
bash tools/regression/run-check.sh
```

## 4. 工程结构总览

```text
groma/
├── pom.xml                     # 根工程 POM：JDK 21 + Spring Boot 4.1.0 + 全局 BOM
├── groma-base/                 # 第一层：5 个公共底座子模块
│   ├── groma-base-common/      # Result/PageResult、@GromaMeta、时间换算、带种子采样、四类关系容器
│   ├── groma-base-engine/      # 单写者 TickEventLoopGroup、MpscRequestBridge、存档续跑 archive 包
│   ├── groma-base-websocket/   # 频道房间、零观察者静默、慢客户端单槽合并、上行指令分发
│   ├── groma-base-connector/   # 表达式沙盒、只读参数化直连库、白名单脚本/MCP 客户端、不可变批次落库
│   └── groma-base-mcp/         # Streamable HTTP /mcp、18 固定工具、7 类只读资源、动态工具注册
├── groma-studio/               # 第二层：7 个建模编译与热更新套件
│   ├── groma-model-import/     # 只读拉取器、SHA-256 不可变语料库、29 形状覆盖度量
│   ├── groma-model-parser/     # 29 连线/21 事件映射表、GromaModuleIR、环检测、静态校验规则引擎
│   ├── groma-naming-engine/    # 四级确定性中英命名、阿里巴巴规约守卫、groma-symbols.lock.json
│   ├── groma-source-generator/ # 五层 JavaPoet 生成器、AstSourceMap、MCP manifest 与双语索引
│   ├── groma-workspace-sync/   # 内存 VFS、@GromaUserCode 保护区无损合并、SHA-256 增量落盘
│   ├── groma-compiler-hotswap/ # 内嵌 ECJ、javac 交叉验证、Child-First 类加载器、版本随会话热切换
│   ├── groma-debug-server/     # DAP 协议路由、条件/日志/异常断点、时统冻结补偿、JDI 沙盒池
│   └── groma-studio-web/       # Vue 3.5 + TS 前端工作台（水位线保存、画布守卫、DAP 联动 Store）
├── groma-models/               # 第三层：编译产出的独立模型子模块（支持 -Psingle-model 单模型裁剪）
│   ├── model-emergency-deduction/  # 应急决策推演（三站两区间积水与列车运行，v825）
│   └── model-smart-planning/       # 智能筹划应用（多智能体串联 + H 时同步航路算法，v844）
├── groma-bootstrap/            # 第四层：Spring Boot 统一运行宿主 + 治理组件
├── docs/                       # 7 份规范强制评审文档（校验规则/字典/线程模型/WS 协议/Redis 键/测试/网关交接）
└── tools/regression/           # 11+ 个自动化验收与回归脚本（scan.py 黄金门禁、run-check.sh 总控）
```
