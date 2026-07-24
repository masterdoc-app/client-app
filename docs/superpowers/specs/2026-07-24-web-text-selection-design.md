# Web text selection

## Goal

Allow users of `https://app.fixaverse.ru/` to select any visible text with a
pointer and copy it with the browser's standard `Ctrl/Cmd+C` shortcut.
Android and Desktop behavior must remain unchanged.

## Design

Add a small platform-specific composable around the application content:

- the Wasm implementation wraps its content in Compose
  `SelectionContainer`;
- Android and Desktop implementations render the content unchanged;
- both regular and authenticated application entry points use the wrapper, so
  loading, error, navigation, and feature screens behave consistently.

This root-level wrapper is preferred over changing individual `Text` calls.
It covers existing direct Material `Text` usage and future screens without an
ongoing audit. CSS `user-select` is not suitable because Compose Wasm text is
not exposed as ordinary selectable HTML text.

Text fields retain their native editing and selection behavior. Clicking
buttons and navigation remains unchanged; dragging over their labels may
select those labels because the requirement covers all visible text.

## Verification

GitHub Actions performs the project build after push. Once deployed, browser
smoke testing verifies:

1. body and heading text can be selected by dragging;
2. `Ctrl/Cmd+C` copies the selected text and it can be pasted;
3. buttons and navigation still respond to clicks;
4. text fields remain editable and their contents remain selectable;
5. the browser console has no new errors.

