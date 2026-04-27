# Feedback Guide

> **A good feedback = developers can immediately understand the problem and start working.** This document defines the standard format for user feedback, helping users write clear, actionable feedback and helping developers quickly locate and resolve issues.

---

## 1. Why We Need a Feedback Standard

Vague feedback leaves developers clueless:

| ❌ Vague Feedback | ✅ Clear Feedback |
|-------------------|-------------------|
| "App doesn't work well" | "Crashes 3 seconds after opening a webpage on Android 14 (screenshot + log attached)" |
| "Network issues" | "Images fail to load on mobile data, works fine on Wi-Fi (screenshot attached)" |
| "Add a feature" | "Would like custom DNS support, e.g. Cloudflare DoH, to bypass ISP DNS pollution" |

**Good feedback enables developers to:**
1. Immediately understand the problem
2. Know how to reproduce it
3. Understand the expected outcome
4. Start working on a fix

---

## 2. Standard Feedback Format

Every feedback should contain the following structure:

```markdown
## Title

[Type] Brief description of the issue or request

Type prefixes:
- [Bug] — Malfunction, crash, unexpected behavior
- [Feature] — New feature request
- [Enhancement] — Improvement to existing feature
- [Question] — Usage question

## Description

Clearly explain what you encountered or what you'd like.

## Reproduction Steps (Required for Bugs)

1. ...
2. ...
3. ...

## Expected Behavior

What you expected to happen.

## Actual Behavior

What actually happened.

## Environment Info

- Device model:
- Android version:
- App version:
- Network (Wi-Fi / Mobile data / Proxy):

## Screenshots / Screen Recording

[Attach relevant screenshots or recordings]

## Additional Info

Anything else you think might be helpful.
```

---

## 3. Excellent Feedback Example

Below is a real Feature Request with complete structure and clear expression — the gold standard for feedback:

> **Custom DNS Support #74** — @BlazeVertex
>
> ### Description
>
> First of all, thanks for this amazing project it's really useful and powerful.
>
> I would like to request a feature related to network access and restrictions.
>
> Currently, when accessing certain websites, they may be blocked or restricted depending on the user's ISP or region. The only workaround is to use a VPN connection, which is not always ideal (battery usage, speed loss, extra setup, etc.).
>
> ### Feature Idea
>
> Would it be possible to add support for custom DNS configuration inside the app?
>
> For example:
> - Allow users to set a custom DNS provider (e.g., Cloudflare DNS, etc.)
> - Option to apply DNS per app instance (per created web app)
> - Possibly support DNS-over-HTTPS (DoH) or DNS-over-TLS (DoT)
>
> ### Benefits
>
> - Access to blocked or restricted websites without needing a full VPN
> - Better performance compared to VPN in many cases
> - More control over privacy
> - Fits well with existing features like hosts blocking and privacy protection
>
> ### Possible Implementation Ideas
>
> - Integrate a lightweight DNS resolver or allow system DNS override within WebView/GeckoView
> - Add DNS settings in the app configuration screen
> - Optional toggle per project/app
>
> ### Additional Context
>
> The project already includes features like:
> - Hosts blocking
> - Privacy protection
> - Custom WebView/GeckoView engines and ad blocking
>
> So adding DNS control would be a natural extension of these capabilities.

**Why this feedback is great:**

| Element | How it's demonstrated |
|---------|----------------------|
| **Background** | Explains the current problem (ISP/region restrictions) and shortcomings of existing solutions (VPN downsides) |
| **Specific request** | Lists 3 clear points: custom provider, per-app config, DoH/DoT support |
| **Value argument** | Lists 4 benefits, helping developers understand "why build this" |
| **Implementation suggestions** | Provides ideas without being prescriptive, respecting developer's technical judgment |
| **Related context** | Points out connection to existing features (hosts blocking, privacy), helping developers locate |

---

## 4. Screenshot & Screen Recording Guidelines

### 4.1 When screenshots are needed

| Scenario | Screenshot needed? |
|----------|-------------------|
| UI display issues (misalignment, overlap, text truncation) | ✅ Required |
| Crashes | ✅ Required (crash screenshot + log) |
| Feature behaves unexpectedly | ✅ Recommended (annotate expected vs actual) |
| Network-related errors | ✅ Required (error message screenshot) |
| New feature request | ⬜ Not needed (describe in text) |
| Performance issues | ⬜ Recommended screen recording to show lag |

### 4.2 Screenshot requirements

- **Highlight key areas**: Use red boxes or arrows to point out the issue — don't make developers guess "what am I looking at"
- **Full screen**: Don't crop to just the problem area — keep context (status bar, navigation, surrounding UI)
- **Multiple comparisons**: If showing "expected vs actual", include both images with labels

### 4.3 Screenshot example

```
❌ Bad screenshot: Only the middle portion, unclear which page or what action triggered it
✅ Good screenshot: Full page + red box highlighting the error + annotation "Should show XX but actually shows YY"
```

### 4.4 Screen recording requirements

- **Show complete operation flow**: From opening the app to triggering the issue, every step
- **Keep under 30 seconds**: Only show key operations, skip irrelevant content
- **Note key moments**: If the recording is long, add text noting "issue appears at XX seconds"

---

## 5. Key Points by Feedback Type

### Bug Feedback

**Core principle: Enable developers to 100% reproduce the issue.**

Required fields:
- **Reproduction steps**: Step-by-step, as if teaching someone who's never used the app
- **Environment info**: Device, OS version, app version, network
- **Actual vs Expected behavior**: Clear comparison

```markdown
## Reproduction Steps

1. Open the app, navigate to [a specific WebApp]
2. Tap the top-right menu → Settings
3. Enable "Background Run"
4. Press Home to return to launcher
5. Wait 5 minutes, then reopen the app
6. App has been killed by the system, page reloads from scratch

## Expected Behavior
After pressing Home, the app should continue running in the background. Reopening should restore the previous page state.

## Actual Behavior
App is killed by the system. Reopening loads from the beginning.

## Environment Info
- Device: Xiaomi 14
- Android 14 (HyperOS 1.0)
- App version: 1.7.0
- Network: Wi-Fi
```

### Feature Request Feedback

**Core principle: Explain "why you need it" and "what you want".**

Required fields:
- **Background/pain point**: What problem are you facing? Why aren't existing solutions enough?
- **Specific request**: What should the app do? Be as specific as possible
- **Value/benefits**: Would this feature help other users too?

Follow the #74 example format: Description → Feature Idea → Benefits → Possible Implementation Ideas → Additional Context

### Enhancement Feedback

**Core principle: Explain "what's wrong now" and "how to improve it".**

Required fields:
- **Current state**: The current behavior and its shortcomings
- **Improvement suggestion**: How you'd like it to work instead
- **Rationale**: Why this change would be better

---

## 6. Feedback Channels

| Channel | Best for |
|---------|----------|
| GitHub Issues | Bugs, Feature Requests, Enhancements — formal tracking |
| Community/Chat | Quick questions, usage help, lightweight discussion |
| Email | Privacy-sensitive issues (e.g., account problems) |

**Please submit formal feature requests and bug reports via GitHub Issues** — this ensures complete tracking records and nothing gets lost.

---

## Revision History

| Date | Changes | Author |
|------|---------|--------|
| 2026-04-25 | Initial English version | Cascade |
