# Wta Design System

This package is the single source of truth for the WebToApp UI. Every new screen
and every new component must consume it; nothing outside `com.webtoapp.ui.design`
should define shapes, spacing, button styling, card styling, or bespoke color
tokens.

## Quick tour

### Tokens (`WtaTokens.kt`)
- `WtaSpacing` — Tiny / Small / Medium / Large / ExtraLarge plus screen-scoped
  aliases (`ScreenHorizontal`, `SectionGap`, `RowHorizontal`, etc.)
- `WtaRadius` — `Card`, `Button`, `Control`, `IconPlate`, `Chip`, `Badge`,
  `Dialog`
- `WtaSize` — `Icon`, `IconSmall`, `IconLarge`, `IconPlate`, `RowMinHeight`,
  `ButtonHeight{Small|Medium|Large}`, `TouchTarget`, `Avatar{Small|Medium|Large}`
- `WtaElevation` — `Level0..Level4`
- `WtaAlpha` — `Disabled`, `Divider`, `MutedContainer`, `PressedContainer`,
  `Subtle`, `Medium`, `Strong`

Never hard-code a `dp`/`sp`/`Color()` value that maps to one of these tokens.

### Colors
- Prefer `MaterialTheme.colorScheme.*` for structural surfaces, text, and
  container semantics.
- Prefer `WtaColors.semantic.*` (`success`, `warning`, `error`, `info`, `neutral`)
  for state accents. Each semantic tone ships with `{onX}` and `{xContainer}`
  variants mirroring Material 3.
- The single theme is `AppThemes.KimiNoNawa`: a calibrated monochrome palette
  with separate light and dark schemes. Dynamic color is opt-in per app policy.
- `AppColors` only holds colors that must stay constant across themes (editor
  chrome, Catppuccin presets, language/brand identity aliases). It is not a
  general-purpose color source.

### Typography
`WtaTypography` in `ui/theme/Typography.kt` wires a custom Material 3 type
scale. Access it via `MaterialTheme.typography.*` the normal way. Do not set
`fontSize` / `lineHeight` ad-hoc; pick a scale entry.

### Layout primitives
- `WtaScreen { padding -> ... }` — top-level screen wrapper with top bar, back
  button, snackbar slot, FAB slot, and the themed background. Replaces any
  hand-rolled `Scaffold` with a `TopAppBar`.
- `WtaBackground { ... }` — themed background alone. Use only when `WtaScreen`
  is not a fit.
- `WtaSection` — titled, optionally collapsible section. Use it to group related
  cards under a heading.

### Settings building blocks
- `WtaSettingCard { ... }` — neutral container for one group of related rows.
- `WtaSettingRow(title, subtitle, icon, trailing = { ... })` — generic row.
- `WtaToggleRow(title, checked, onCheckedChange)` — row with a trailing
  `WtaSwitch`.
- `WtaChoiceRow(title, value, onClick)` — row that opens a picker.
- `WtaTextFieldRow(title, value, onValueChange)` — inline labeled input.
- `WtaSliderRow(title, value, onValueChange)` — inline slider with optional
  value label.
- `WtaDangerRow(title, onClick)` — destructive action row with security tag.
- `WtaSectionDivider()` — 1px inset divider for inside cards.

### Banners and empty states
- `WtaStatusBanner(message, tone = Info|Success|Warning|Error, actionLabel,
  onAction)` — contextual notice at the top of a screen.
- `WtaEmptyState(title, message, icon, actionLabel, onAction)` — when a card's
  interior should display an empty placeholder.
- `WtaFullEmptyState(title, message, icon, action)` — screen-wide empty state.
- `WtaLoadingState(message)` — centered spinner with optional caption.
- `WtaErrorState(message, retryLabel, onRetry)` — centered error with retry.

### Buttons
- `WtaButton(onClick, text, variant, size, leadingIcon, trailingIcon)` — text +
  icon convenience form.
- `WtaButton(onClick, variant, size) { content }` — slotted form.
- `WtaIconButton(onClick, icon, contentDescription, tonal)` — icon-only button.

Variants: `Primary` (filled), `Tonal`, `Outlined`, `Text`, `Destructive`.
Sizes: `Small` (36dp), `Medium` (44dp), `Large` (52dp).

### Inputs & selection
- `WtaTextField(value, onValueChange, label, placeholder, leadingIcon,
  trailingIcon, supportingText, isError)` — outlined input.
- `WtaSwitch(checked, onCheckedChange)` — the only toggle primitive.
- `WtaChip(selected, onClick, label, leadingIcon)` — selectable chip.
- `WtaInfoChip(label, icon)` — read-only badge for metadata.

### Display helpers
- `WtaIconTitle(icon, title, subtitle)` — icon plate + title header.
- `WtaStatItem(value, label, accent)` — dashboard stat block.
- `WtaBadge(text, icon)` — small pill badge.
- `WtaDivider()` — full-width themed divider.

### Behavior
- `rememberHapticClick { ... }` — wraps a click handler with a standard haptic
  pulse respecting the user's animation settings.
- `Modifier.wtaPressScale(interactionSource)` — 3 % press scale used by
  buttons and clickable cards. Apply sparingly; prefer the built-in Material
  state layer.

## Migration status

- `ThemedBackgroundBox`, `GradientButton`, `GlowingButton`, `GlassmorphicCard`,
  `FloatingCard`, `GradientBorderCard`, the themed loading / badge / divider
  helpers — **deleted**.
- `PremiumSwitch` — deleted; call sites migrated to `WtaSwitch`.
- `PremiumButton`, `PremiumOutlinedButton`, `PremiumTextField`,
  `PremiumFilterChip`, `EnhancedElevatedCard`, `EnhancedOutlinedCard`,
  `SettingsSwitch`, `IconSwitchCard`, `IconTitleRow`, `CollapsibleCardHeader`,
  `WarningCard`, `EmptyStatePlaceholder`, `LoadingPlaceholder`,
  `ErrorPlaceholder`, `InfoChip`, `StatItem`, `CardContent(WithSpacing)` —
  retained as permanent **alias** layers over Wta internals. They are not
  deprecated: older screens that composed their own slot content keep using the
  slot-style Material API; everything renders through Wta so the visuals are
  already unified.

## When to fall back to Material 3 directly

It is fine to use `Card`, `Surface`, `Text`, `Icon`, `TextButton`, etc. directly
when you need a one-off Material-flavored surface and none of the Wta wrappers
fit. Even then:
- Pull sizes from `WtaSpacing` / `WtaSize`.
- Pull shapes from `WtaRadius` wrapped in `RoundedCornerShape(...)`.
- Pull colors from `MaterialTheme.colorScheme.*` or `WtaColors.semantic.*`.
- Never hard-code hex colors outside `AppThemes`, `AppColors`, and the special
  UI callouts in this doc.
