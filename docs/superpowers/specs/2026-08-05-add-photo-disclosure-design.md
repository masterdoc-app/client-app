# Add photo disclosure

date: 2026-08-05  
repo: `client-app`

## Goal

Reduce visual noise in the new work order form by placing the photo-source actions behind one clear action.

## UI behavior

- Show a full-width secondary button labeled «Добавить фото».
- Clicking it toggles an inline row with «С диска» and «Камера».
- Keep the existing image previews, removal behavior, upload flow, errors, and 10-photo limit.
- Disable photo controls while the form is unavailable, an action is running, or the photo limit is reached.
- Keep the source row visible until the user closes it with the same toggle or leaves the form.

## Scope

Only the new work order form in `TicketsScreen` changes. Work order details and comment attachments remain unchanged.

## Testing

- Unit-test the pure disclosure state transition.
- Verify the deployed form visually and confirm both source actions still invoke their existing pickers.
