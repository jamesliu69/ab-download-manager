# Domain docs

說明工程技能在探索程式碼前，應如何閱讀本儲存庫的領域文件。

## 開始探索前先閱讀

- 根目錄的 `GLOSSARY.md`；或
- 若根目錄存在 `GLOSSARY-MAP.md`，依照它找到各領域的 `GLOSSARY.md`，並閱讀與目前主題相關的詞彙表。
- `docs/adr/` 中與目前工作範圍相關的 ADR。

若以上檔案不存在，請直接繼續，不必特別指出缺少文件或預先建議新增。`/domain-modeling`（由 `/grill-with-docs` 或 `/improve-codebase-architecture` 使用）會在領域詞彙或決策實際定案時才建立文件。

## 檔案配置

本儲存庫採用 single-context 配置：在根目錄放置一份 `GLOSSARY.md`，並將 ADR 放在 `docs/adr/`。

```text
/
├── GLOSSARY.md
├── docs/adr/
│   ├── 0001-example-decision.md
│   └── 0002-another-decision.md
└── ...
```

## 使用詞彙表中的用語

在 issue 標題、重構提案、假設或測試名稱中提及領域概念時，使用 `GLOSSARY.md` 定義的詞彙。不要改用詞彙表明確避免的同義詞。

若需要的概念尚未列入詞彙表，請重新確認是否引入了專案未使用的名稱；若確實是缺漏，記錄下來供 `/domain-modeling` 處理。

## 指出 ADR 衝突

若輸出內容與既有 ADR 衝突，請明確指出，不要默默推翻決策，例如：

> _這與 ADR-0007（事件溯源訂單）相衝突，但值得重新檢視，因為……_
