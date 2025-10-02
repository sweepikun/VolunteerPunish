# VolunteerPunish 志愿者处罚系统

VolunteerPunish 是一个为 Minecraft 服务器设计的志愿者处罚管理系统。该插件允许服务器管理员授权特定玩家执行有限的处罚操作（如封禁和禁言），同时通过配额系统控制处罚次数，防止滥用权力。

## 功能特点

- **志愿者权限管理**：通过命令设置和移除志愿者身份
- **处罚配额系统**：为不同级别的志愿者设置每日处罚次数限制
- **多种处罚类型**：支持封禁（ban）和禁言（mute）两种处罚方式
- **灵活的时长配置**：可为不同组别设置可用的处罚时长选项
- **数据库支持**：支持 SQLite 和 MySQL 数据库存储数据
- **权限插件集成**：可选集成 LuckPerms 权限插件
- **自动配额重置**：支持定时自动重置志愿者每日配额
- **完整的命令补全**：提供全面的命令自动补全功能
- **处罚历史记录**：可查看玩家的处罚历史
- **自定义通知消息**：支持自定义处罚通知的Title、ActionBar和聊天消息
- **自定义身份组**：支持自定义志愿者身份组，可配置不同组别的权限和配额

## 命令列表

| 命令 | 权限节点 | 说明 |
|------|----------|------|
| `/vp ban <玩家> <时长\|permanent> [原因]` | `volunteerpunish.volunteer.ban` | 封禁玩家 |
| `/vp mute <玩家> <时长\|permanent> [原因]` | `volunteerpunish.volunteer.mute` | 禁言玩家 |
| `/vp unban <玩家>` | `volunteerpunish.admin.unban` | 解封玩家 |
| `/vp unmute <玩家>` | `volunteerpunish.admin.unmute` | 解除玩家禁言 |
| `/vp history [玩家]` | `volunteerpunish.admin.history` | 查看处罚历史记录 |
| `/vp setid <玩家> <ID>` | `volunteerpunish.admin.setid` | 设置志愿者ID |
| `/vp removeid <玩家>` | `volunteerpunish.admin.removeid` | 移除志愿者身份 |
| `/vp group` | - | 查看自己的身份组 |
| `/vp group <志愿者ID> <组名>` | `volunteerpunish.admin.setgroup` | 修改志愿者身份组 |
| `/vp reload` | `volunteerpunish.admin.reload` | 重新加载配置文件 |
| `/vp help` | - | 显示帮助信息 |

## 权限节点

- `volunteerpunish.volunteer.ban` - 允许执行封禁操作
- `volunteerpunish.volunteer.mute` - 允许执行禁言操作
- `volunteerpunish.admin.unban` - 允许解封玩家
- `volunteerpunish.admin.unmute` - 允许解除禁言
- `volunteerpunish.admin.setid` - 允许设置志愿者ID
- `volunteerpunish.admin.removeid` - 允许移除志愿者身份
- `volunteerpunish.admin.setgroup` - 允许设置志愿者身份组
- `volunteerpunish.admin.history` - 允许查看处罚历史
- `volunteerpunish.admin.reload` - 允许重新加载配置

## 配置文件

插件的配置文件位于 `plugins/VolunteerPunish/config.yml`：

```yaml
# VolunteerPunish 配置文件

# 数据库配置
database:
  # 数据库类型: sqlite 或 mysql
  type: sqlite
  
  # SQLite 配置
  sqlite:
    path: database.db
  
  # MySQL 配置
  mysql:
    host: localhost
    port: 3306
    database: volunteerpunish
    username: root
    password: ''

# 身份组配置
groups:
  # 初级志愿者
  beginner:
    # 配额设置
    quotas:
      ban: 3      # 每日封禁配额
      mute: 5     # 每日禁言配额
    
    # 可用的处罚时长（秒）
    durations:
      ban:
        - 3600    # 1小时
        - 21600   # 6小时
        - 86400   # 1天
        - 604800  # 7天
      mute:
        - 600     # 10分钟
        - 3600    # 1小时
        - 7200    # 2小时
        - 21600   # 6小时
    
    # 是否允许自定义原因
    reasons:
      ban: true
      mute: true
  
  # 中级志愿者
  intermediate:
    quotas:
      ban: 5
      mute: 10
    durations:
      ban:
        - 3600    # 1小时
        - 21600   # 6小时
        - 86400   # 1天
        - 259200  # 3天
        - 604800  # 7天
        - 2592000 # 30天
      mute:
        - 600     # 10分钟
        - 3600    # 1小时
        - 7200    # 2小时
        - 21600   # 6小时
        - 86400   # 1天
    reasons:
      ban: true
      mute: true

# 每日配额重置设置
daily-reset:
  enabled: true
  timezone: UTC

# 处罚通知设置
notification:
  # 玩家登录时是否显示处罚通知
  login:
    enabled: true
    
  # 通知消息模板（支持颜色代码）
  messages:
    ban: "&c你已被志愿者 #{volunteer_id} 封禁 {duration}，原因：{reason}。解封时间：{unban_time}"
    mute: "&c你已被志愿者 #{volunteer_id} 禁言 {duration}，原因：{reason}"
    
  # Title通知设置
  title:
    # 封禁时显示的Title
    ban:
      title: "&c你已被封禁"
      subtitle: "&7原因: {reason}"
    # 禁言时显示的Title
    mute:
      title: "&c你已被禁言"
      subtitle: "&7原因: {reason}"
      
  # ActionBar通知设置
  actionbar:
    # 封禁时显示的ActionBar消息
    ban: "&c你当前处于封禁状态"
    # 禁言时显示的ActionBar消息
    mute: "&c你当前处于禁言状态"
```

### 自定义身份组配置说明

VolunteerPunish 支持完全自定义的身份组配置，您可以根据服务器需求创建任意数量和类型的志愿者组。每个组可以独立配置以下属性：

1. **配额设置 (quotas)**
   - `ban`: 每日封禁配额
   - `mute`: 每日禁言配额

2. **时长选项 (durations)**
   - `ban`: 该组可用的封禁时长列表（以秒为单位）
   - `mute`: 该组可用的禁言时长列表（以秒为单位）

3. **原因设置 (reasons)**
   - `ban`: 是否允许自定义封禁原因
   - `mute`: 是否允许自定义禁言原因

要添加新的身份组，只需在 `groups` 部分下添加新的配置块，例如：

```yaml
groups:
  # 高级志愿者组
  advanced:
    quotas:
      ban: 10
      mute: 20
    durations:
      ban:
        - 3600    # 1小时
        - 21600   # 6小时
        - 86400   # 1天
        - 604800  # 7天
        - 2592000 # 30天
      mute:
        - 600     # 10分钟
        - 3600    # 1小时
        - 7200    # 2小时
        - 21600   # 6小时
        - 86400   # 1天
        - 604800  # 7天
    reasons:
      ban: true
      mute: true
```

## 安装与使用

1. 将插件jar文件放入服务器的 `plugins` 文件夹中
2. 启动服务器以生成默认配置文件
3. 根据需要修改 `config.yml` 配置文件
4. 重新启动服务器或使用 `/vp reload` 命令重新加载配置
5. 使用 `/vp setid <玩家> <ID>` 命令将玩家设置为志愿者
6. 志愿者可以使用 `/vp ban` 和 `/vp mute` 命令执行处罚

## 权限插件集成

VolunteerPunish 支持与 LuckPerms 权限插件集成。当服务器安装了 LuckPerms 时，插件会自动管理志愿者权限：
- 使用 `/vp setid` 命令设置志愿者时会自动添加权限
- 使用 `/vp removeid` 命令移除志愿者时会自动移除权限

## 构建项目

```bash
# 克隆项目
git clone https://github.com/sweepikun/VolunteerPunish.git

# 进入项目目录
cd VolunteerPunish

# 构建项目
./gradlew build

# 构建包含所有依赖的fat jar（可选）
./gradlew fatJar
```

## 依赖项

- Spigot/Paper API 1.16.5 或更高版本
- LuckPerms API（可选，用于权限管理）
- HikariCP（用于数据库连接池）
- BungeeCord Chat API（用于ActionBar消息）

## 许可证

本项目采用 [GPL-v3 License](LICENSE) 开源许可证。

## 贡献

欢迎提交 Issue 和 Pull Request 来帮助改进这个插件。