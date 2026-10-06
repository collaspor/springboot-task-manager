# Spring Boot 全栈任务管理练习

这是一个可以直接运行的入门项目：Spring Boot 提供 REST API，Vue 3 页面调用 API，Spring Data JPA 将任务保存到 H2 文件数据库。第一版专注任务的新增、查询、编辑、完成状态和删除，不包含登录。

## 运行环境

- Java 21、Maven 3.9+、Node.js 20.19+、npm。
- 本机已验证的版本为 Java 21、Maven 3.9.11、Node.js 24.14.1。
- 首次执行 `mvn` 和 `npm install` 需要下载依赖。

## 启动项目

在 PowerShell 中打开两个终端。命令均从项目根目录 `D:\Dev\Projects\springboot-task-manager` 开始。

**终端一：启动后端**

```powershell
cd D:\Dev\Projects\springboot-task-manager\backend
mvn -s ..\maven-settings.xml spring-boot:run
```

看到 `Started TaskManagerApplication` 后，后端位于 `http://localhost:8081`。`maven-settings.xml` 将依赖缓存放在项目内的 `.m2`，避免修改本机全局 Maven 仓库。

**终端二：启动前端**

```powershell
cd D:\Dev\Projects\springboot-task-manager\frontend
npm install --cache ..\.npm-cache
npm run dev
```

浏览器打开 `http://127.0.0.1:5173`。首次启动页面为空列表；新增一个任务，刷新页面仍可看到它。后端启动时的工作目录应为 `backend`，这样数据库会写到 `backend/data/tasks.mv.db`。数据库文件已被 Git 忽略。

## 项目结构

```text
springboot-task-manager/
├─ backend/
│  ├─ pom.xml                       Maven 依赖与 Spring Boot 版本
│  ├─ src/main/java/.../            Controller、Service、Repository、Entity
│  ├─ src/main/resources/            数据库与服务器配置
│  ├─ src/test/                      API 集成测试
│  └─ data/                          H2 数据文件（运行后生成）
├─ frontend/
│  ├─ src/App.vue                    页面及交互
│  ├─ src/api.js                     HTTP 请求
│  ├─ src/style.css                  页面样式
│  └─ vite.config.js                 开发时的 /api 代理
└─ maven-settings.xml                项目专用 Maven 缓存位置
```

## 一次请求如何走完整个后端

1. 在页面输入标题并点击“添加任务”。`App.vue` 调用 `api.js`，向 `POST /api/tasks` 发送 JSON。
2. `TaskController` 接收并验证请求。标题不能为空，最多 100 字；描述最多 1000 字。
3. `TaskService` 处理业务操作，创建 `Task` 实体，再调用 `TaskRepository`。
4. Spring Data JPA 根据 Repository 接口执行数据库操作。H2 将数据保存在文件中。
5. Controller 返回任务 JSON；前端重新获取列表并更新页面。刷新页面时，前端会再次调用 `GET /api/tasks`。

这条链路可以记作：**Vue 页面 → HTTP → Controller → Service → Repository → H2**。

## API 速查

| 方法 | 地址 | 用途 |
| --- | --- | --- |
| GET | `/api/tasks?status=all` | 查询全部任务；`status` 也可为 `active` 或 `completed` |
| POST | `/api/tasks` | 新增任务，请求体含 `title`、可选 `description` |
| PUT | `/api/tasks/{id}` | 更新任务，请求体含 `title`、`description`、`completed` |
| DELETE | `/api/tasks/{id}` | 删除任务 |

新增成功返回 201，删除成功返回 204；无效输入返回 400，任务不存在返回 404。列表按创建时间从新到旧排列。

## 测试与构建

```powershell
cd D:\Dev\Projects\springboot-task-manager\backend
mvn -s ..\maven-settings.xml test

cd D:\Dev\Projects\springboot-task-manager\frontend
npm test
npm run build
```

后端集成测试使用内存 H2，不会改动平时使用的 `backend/data`。前端 `npm test` 检查 API 请求格式和错误处理，`npm run build` 检查页面能否打包。

## 建议的练习顺序

1. 先运行项目，在浏览器中完成一轮新增、编辑、筛选、标记完成、删除。
2. 从 `TaskController` 的 `POST` 方法开始，沿着 `TaskService`、`TaskRepository`、`Task` 读一次代码，再回到 `App.vue` 看请求如何发出。
3. 在 `TaskApiTest` 中增加一个测试，例如标题超过 100 字时返回 400；先运行测试，再观察验证注解如何发挥作用。
4. 尝试增加“优先级”字段：先修改实体和请求/响应，再修改 Service、测试与页面。改动之前先理解现有函数逻辑，保留原有行为。

新增中文注释请使用 UTF-8 保存，并检查终端和浏览器中是否出现乱码。
