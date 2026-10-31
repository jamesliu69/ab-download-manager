# Issue tracker：GitHub

本儲存庫的問題與規格以 GitHub Issues 管理。所有操作使用 `gh` CLI。

## 慣例

- **建立 issue**：`gh issue create --title "..." --body "..."`。多行內文使用 here-string 或其他安全的多行輸入方式。
- **讀取 issue**：`gh issue view <number> --json number,title,body,labels,comments`。
- **列出 issue**：`gh issue list --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'`，並依需要加入 `--label`、`--state` 篩選。
- **建立子 issue**：`gh issue create --parent <parent> ...`，或之後對父 issue 執行 `gh issue edit <parent> --add-sub-issue <child>`（需要 gh 2.94 以上版本）。若不支援子 issue，請在子 issue 內文開頭加上 `Part of #<parent>`。
- **留言**：`gh issue comment <number> --body "..."`。
- **新增／移除標籤**：`gh issue edit <number> --add-label "..."`／`--remove-label "..."`。
- **關閉**：`gh issue close <number> --comment "..."`。

在複製儲存庫內執行時，`gh` 會依 Git remote 推斷儲存庫。

## 將 Pull request 作為需求來源

**將 PR 作為需求來源：否。**（若此儲存庫將外部 PR 視為功能需求，可改為 `是`；`/triage` 會讀取此設定。）

若改為 `是`，PR 也使用與 issue 相同的標籤與狀態，並使用對應的 `gh pr` 指令：

- **讀取 PR**：`gh pr view <number> --comments`，並以 `gh pr diff <number>` 查看差異。
- **列出外部 PR**：`gh api --paginate 'repos/{owner}/{repo}/pulls?state=open' --jq '.[] | select(.author_association | IN("OWNER","MEMBER","COLLABORATOR") | not) | {number, title, author: .user.login, author_association, labels: [.labels[].name]}'`。
- **留言／標籤／關閉**：使用 `gh pr comment`、`gh pr edit --add-label`／`--remove-label`、`gh pr close`。

GitHub 的 issue 與 PR 共用編號；若只收到 `#42`，先執行 `gh pr view 42`，再視需要改查 `gh issue view 42`。

## 技能如何使用 issue tracker

- 技能要求「發布到 issue tracker」時，建立 GitHub issue。
- 技能要求「取得相關 ticket」時，使用上述讀取 issue 的指令。

## Wayfinding 慣例

以下供 `/wayfinder` 使用。

- **Map**：建立單一 issue，標上 `wayfinder:map`，並記錄 Notes／Decisions-so-far／Fog。使用 `gh issue create --label wayfinder:map`。
- **子 ticket**：建立連結到 map 的 GitHub 子 issue（見上方指令），並加上 `wayfinder:<type>` 標籤（`research`／`prototype`／`grilling`／`task`）。若無法使用子 issue，在 map 內文加入子項目清單，並在子 issue 開頭加上 `Part of #<map>`。被認領後，指派給負責推進的開發者。
- **阻擋關係**：以 GitHub 原生 issue dependency 作為標準且可在 UI 檢視的表示法。使用 `gh api --method POST repos/<owner>/<repo>/issues/<child>/dependencies/blocked_by -F issue_id=<blocker-db-id>`；`<blocker-db-id>` 是阻擋 issue 的數字資料庫 ID（不是 `#number` 或 `node_id`）。可用 `gh api repos/<owner>/<repo>/issues/<n> --jq .id` 取得。GitHub 會以 `issue_dependencies_summary.blocked_by` 回報未關閉的阻擋項目。若無法使用 dependencies，改在子 issue 開頭加入 `Blocked by: #<n>, #<n>`。
- **Frontier 查詢**：依 map 順序查看尚未關閉的子項目，略過仍有未關閉阻擋項目或已指派的 ticket。先使用原生 `issue_dependencies_summary.blocked_by`；若無法使用，檢查 `Blocked by` 行中是否有未關閉 issue。
- **認領**：使用 `gh issue edit <n> --add-assignee @me`；這是本工作階段對該 ticket 的第一個寫入操作。
- **解決**：先以 `gh issue comment <n> --body "<answer>"` 回覆，再執行 `gh issue close <n>`，最後在 map 的 Decisions-so-far 附上脈絡連結（gist 與連結）。
