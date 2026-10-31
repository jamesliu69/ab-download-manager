# Agent instructions

## 保護上游倉庫

所有工作僅限本機工作樹及非上游目標。先檢查 Git remote URL，確認目標不是上游倉庫；絕不向上游推送提交，或透過 GitHub、Git、CI/CD 等工具在上游建立、修改或刪除任何內容或設定。此限制不因使用者對其他操作的授權而解除。

## Agent skills

### Issue tracker

本儲存庫使用 GitHub Issues 追蹤工作，透過 `gh` CLI 操作。詳見 `docs/agents/issue-tracker.md`。

### Triage labels

使用 `needs-triage`、`needs-info`、`ready-for-agent`、`ready-for-human` 和 `wontfix`。詳見 `docs/agents/triage-labels.md`。

### Domain docs

採用 single-context 領域文件配置。詳見 `docs/agents/domain.md`。
