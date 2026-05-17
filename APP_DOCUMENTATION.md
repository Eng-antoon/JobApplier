# JobApplier - Complete App Documentation for UI/UX Redesign

## Table of Contents

- [App Overview](#app-overview)
- [Tech Stack](#tech-stack)
- [App Flow & Navigation Architecture](#app-flow--navigation-architecture)
- [Color Palette & Theme (Current)](#color-palette--theme-current)
- [Typography](#typography)
- [Reusable Components](#reusable-components)
- [Screen-by-Screen Breakdown](#screen-by-screen-breakdown)
  - [Splash Screen](#1-splash-screen)
  - [Login Screen](#2-login-screen)
  - [Sign Up Screen](#3-sign-up-screen)
  - [Onboarding Choice Screen](#4-onboarding-choice-screen)
  - [Resume Import Screen](#5-resume-import-screen)
  - [Onboarding Screen (Manual)](#6-onboarding-screen-manual-fill)
  - [Dashboard Screen](#7-dashboard-screen-home-tab)
  - [Add Job Screen](#8-add-job-screen)
  - [Job Detail Screen](#9-job-detail-screen)
  - [Profile Screen](#10-profile-screen-profile-tab)
  - [Snippets Screen](#11-snippets-screen-snippets-tab)
  - [AI Suggestions Screen](#12-ai-suggestions-screen-ai-tab)
  - [Bubble Overlay Service](#13-bubble-overlay-floating-widget)
- [Data Models](#data-models)
- [Current Design Problems & Pain Points](#current-design-problems--pain-points)
- [Design Enhancement Goals](#design-enhancement-goals)

---

## App Overview

**JobApplier** is an AI-powered Android application that helps job seekers manage their job applications more efficiently. The app allows users to:

1. **Build a professional profile** — name, skills, work experience, education, certifications, languages, professional summary
2. **Import resume data via AI** — upload PDF/DOCX, AI extracts structured data (skills, experience, education, etc.)
3. **Track job applications** — paste or auto-fetch job descriptions, get AI-powered match scores
4. **Generate tailored content** — cover letters, cover emails, and answers to common interview questions, all tailored to the specific job + user profile
5. **Quick-copy snippets** — one-tap copy of profile data (name, email, skills, etc.) for filling out application forms
6. **Floating bubble overlay** — a system-wide floating widget that provides quick-copy access while using other apps (like filling LinkedIn forms)
7. **AI career suggestions** — profile analysis with headline ideas, summary rewrites, skill gap identification, and general career tips

**Target Audience:** Job seekers actively applying to multiple positions, especially those using LinkedIn and other job platforms.

**Package:** `com.aplicator.jobapplier`
**Min SDK:** 31 (Android 12)
**Target SDK:** 36

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose with Material 3 (Material Design 3) |
| **Navigation** | Jetpack Navigation Compose (type-safe routes via `@Serializable`) |
| **DI** | Hilt (Dagger) |
| **Backend** | Supabase (Auth, PostgREST, Edge Functions) |
| **Network** | Ktor (Android engine) |
| **AI** | Supabase Edge Functions as proxy to AI models |
| **Serialization** | kotlinx.serialization |
| **PDF Parsing** | PdfBox-Android |
| **DOCX Parsing** | Apache POI (poi-ooxml) |
| **Font** | Plus Jakarta Sans (4 weights: Regular, Medium, SemiBold, Bold) |
| **Splash** | AndroidX Core SplashScreen API |

---

## App Flow & Navigation Architecture

```
App Launch
    │
    ▼
[Splash Screen] ──── animated gradient background + pulsing logo + "JobApplier" text
    │
    ▼
Session Check (Supabase Auth)
    │
    ├── NOT Authenticated ──► [Auth Nav Graph]
    │                              ├── [Login Screen] ◄──► [Sign Up Screen]
    │                              └── On success ──► Session Check again
    │
    ├── Authenticated + NOT Onboarded ──► [Onboarding Flow]
    │                                         ├── [Onboarding Choice Screen]
    │                                         │      ├── "Import from Resume" ──► [Resume Import Screen]
    │                                         │      └── "Fill Manually" ──► [Onboarding Screen (4 steps)]
    │                                         └── On complete ──► Main App
    │
    └── Authenticated + Onboarded ──► [Main Nav Graph]
                                          │
                                          ├── Bottom Nav Bar (4 tabs):
                                          │   ├── Home (Dashboard) ──► [Add Job] ──► [Job Detail]
                                          │   ├── Profile ──► [Resume Import]
                                          │   ├── Snippets (Quick Copy)
                                          │   └── AI (Suggestions)
                                          │
                                          └── Floating Action: Bubble Overlay Service
```

### Bottom Navigation Bar

| Tab | Icon | Label | Screen |
|-----|------|-------|--------|
| 1 | `Icons.Default.Home` | "Home" | Dashboard |
| 2 | `Icons.Default.Person` | "Profile" | Profile |
| 3 | `Icons.Default.ContentCopy` | "Snippets" | Quick Copy |
| 4 | `Icons.Default.AutoAwesome` | "AI" | AI Suggestions |

**Current bottom bar:** Standard Material 3 `NavigationBar` — no custom styling, no theming beyond defaults. Uses system Material 3 colors.

**Navigation animations:**
- Bottom nav tabs: fade in/out (300ms)
- Push screens (AddJob, JobDetail, ResumeImport): slide in from right (300ms), slide out to right on pop
- App state transitions (Auth → Main): fade + slight slide up

---

## Color Palette & Theme (Current)

The current theme is described as "LinkedIn-inspired" but feels generic and corporate.

### Light Theme

| Token | Hex | Usage |
|-------|-----|-------|
| Primary | `#0A66C2` | LinkedIn blue — buttons, active states, links |
| OnPrimary | `#FFFFFF` | Text on primary buttons |
| PrimaryContainer | `#D0E4FF` | Light blue — backgrounds for highlighted cards |
| Secondary | `#004182` | Darker blue — secondary actions |
| SecondaryContainer | `#D4E3FF` | Light secondary backgrounds |
| Tertiary | `#057642` | Green — success/positive states |
| TertiaryContainer | `#B8F1CC` | Light green backgrounds |
| Error | `#CC1016` | Red — errors, low match scores |
| Background | `#F4F2EE` | LinkedIn warm gray — main background |
| Surface | `#FFFFFF` | Cards, sheets |
| SurfaceVariant | `#EEF3F8` | Slightly tinted card backgrounds |
| OnSurface | `#191919` | Near-black text |
| OnSurfaceVariant | `#44474B` | Secondary text |

### Dark Theme

| Token | Hex | Usage |
|-------|-----|-------|
| DarkPrimary | `#70B5F9` | Light blue for dark mode |
| DarkBackground | `#1B1F23` | Dark background |
| DarkSurface | `#282D33` | Dark card surfaces |
| DarkSurfaceVariant | `#38434F` | Dark variant surfaces |

### Semantic Colors

| Name | Hex | Usage |
|------|-----|-------|
| MatchHigh | `#057642` | Match score >= 70% (green) |
| MatchMedium | `#B24020` | Match score 40-69% (orange-brown) |
| MatchLow | `#CC1016` | Match score < 40% (red) |

### Design Problems with Current Colors:
- Palette is monotone blue — LinkedIn clone feel, not distinctive
- No gradient accents or vibrant secondary colors
- Warm gray background (#F4F2EE) feels dated
- No brand differentiation — could be any generic business app
- Dark theme is functional but uninspired
- No accent colors for visual interest or delight

---

## Typography

**Font Family:** Plus Jakarta Sans

| Style | Weight | Size | Line Height | Letter Spacing |
|-------|--------|------|-------------|----------------|
| headlineLarge | Bold | 28sp | 36sp | default |
| headlineMedium | SemiBold | 24sp | 32sp | default |
| titleLarge | SemiBold | 20sp | 28sp | default |
| titleMedium | Medium | 16sp | 24sp | 0.15sp |
| bodyLarge | Normal | 16sp | 24sp | 0.5sp |
| bodyMedium | Normal | 14sp | 20sp | 0.25sp |
| labelLarge | Medium | 14sp | 20sp | 0.1sp |
| labelSmall | Medium | 11sp | 16sp | 0.5sp |

**Note:** Plus Jakarta Sans is a good modern font. Consider keeping it but adding more weight variation and possibly a display font for headlines.

---

## Reusable Components

### 1. AnimatedGradientBackground
- **File:** `ui/components/AnimatedGradientBackground.kt`
- **What it does:** Full-screen animated linear gradient that rotates 360 degrees over 8 seconds
- **Default colors:** Dark blue gradient (`#004182` → `#0A66C2` → `#0073B1` → `#004182`)
- **Used in:** Splash screen only
- **Problem:** Only used once. Could be leveraged more throughout the app for premium feel

### 2. GlassmorphismCard
- **File:** `ui/components/GlassmorphismCard.kt`
- **What it does:** Card with shadow, rounded corners (12dp), surface background, subtle border (0.3 alpha)
- **Used in:** Nowhere currently (unused component)
- **Problem:** Built but never integrated. This premium-feeling component should replace generic Cards

### 3. PremiumButton
- **File:** `ui/components/PremiumButton.kt`
- **What it does:** Material button with press animation (scales to 96% on press), 4dp elevation, medium rounded shape
- **Used in:** Resume Import screen only
- **Problem:** Limited usage — should be the primary CTA button throughout the entire app

### 4. PremiumLoadingIndicator
- **File:** `ui/components/PremiumLoadingIndicator.kt`
- **What it does:** Custom animated loading spinner with gradient sweep arc + pulsing circle + optional message text
- **Used in:** Resume Import screen only
- **Problem:** Beautiful custom loader but only used in one place. Dashboard and other screens use basic `CircularProgressIndicator`

### 5. ShimmerEffect (ShimmerBox, ShimmerJobCard, ShimmerProfileSection)
- **File:** `ui/components/ShimmerEffect.kt`
- **What it does:** Skeleton loading placeholders with animated shimmer gradient
- **ShimmerJobCard:** Card skeleton with title, subtitle, label, and score placeholder
- **ShimmerProfileSection:** Card skeleton with title and 4 text lines
- **Used in:** Dashboard (loading state), Profile (loading state), Job Detail (loading state)
- **Problem:** Good skeleton loading pattern, but shimmer colors are hardcoded gray (not theme-aware)

### 6. OverlayPermissionDialog
- **File:** `ui/components/OverlayPermissionDialog.kt`
- **What it does:** AlertDialog explaining why overlay permission is needed, with "Open Settings" button
- **Used in:** Snippets screen
- **Problem:** Standard AlertDialog — no custom styling or branding

### 7. AnimatedSplashContent
- **File:** `ui/components/AnimatedSplashContent.kt`
- **What it does:** Splash screen with animated gradient background, pulsing ring animation, stylized "J" letter drawn on Canvas, fade-in text ("JobApplier" + "Your AI-powered career companion")
- **Used in:** App loading state
- **Problem:** Decent but the Canvas-drawn "J" looks basic. Should use a proper SVG/vector logo

---

## Screen-by-Screen Breakdown

### 1. Splash Screen

**File:** `ui/components/AnimatedSplashContent.kt`
**State:** Shown while Supabase session initializes

**Visual Elements:**
- Full-screen animated gradient background (dark blues rotating)
- Centered content:
  - Pulsing white ring animation (expanding outward, fading)
  - White circle with "J" letter drawn via Canvas paths
  - "JobApplier" text (28sp, Bold, white)
  - "Your AI-powered career companion" subtitle (14sp, Medium, white, 0.7 alpha)
- Animations: logo scales from 0.8→1.0, fades in over 600ms, text fades in 200ms later

**Current Problems:**
- Canvas-drawn "J" looks amateur — needs a proper branded logo/icon
- No loading indicator — user doesn't know if app is loading or stuck
- Tagline is generic
- Gradient is nice but could be more dynamic

---

### 2. Login Screen

**File:** `ui/auth/LoginScreen.kt`
**Route:** `Screen.Login`

**Layout (top to bottom):**
1. **Hero Section (240dp height):**
   - Canvas-drawn background: primary color (#0A66C2) wave shape filling top 75% with curved bottom edge
   - Subtle lighter wave overlay (primaryContainer, 0.3 alpha)
   - Centered on top of canvas:
     - App logo image (72dp, from `R.drawable.ic_app_logo`)
     - "Job Applier" headline (headlineLarge, white)
     - "Land your dream job faster" subtitle (bodyLarge, white, 0.85 alpha)

2. **Form Section (below hero, 24dp horizontal padding):**
   - "Welcome Back" heading (headlineMedium)
   - "Sign in to continue" subtitle (bodyMedium, onSurfaceVariant)
   - 24dp spacer
   - Email `OutlinedTextField` with Email icon, full width
   - 16dp spacer
   - Password `OutlinedTextField` with Lock icon, visibility toggle, full width
   - 24dp spacer
   - "Sign In" `Button` (full width, 50dp height) — shows `CircularProgressIndicator` when loading
   - 16dp spacer
   - "Don't have an account? Sign Up" `TextButton`

3. **Error Handling:** Snackbar for errors

**Current Problems:**
- Canvas wave hero looks decent but is a static painted shape — not premium
- No social login options (Google, LinkedIn)
- No "Forgot Password" link
- Standard OutlinedTextField — no visual differentiation
- Login button is basic Material button, not PremiumButton
- No illustration or animation to make the screen feel alive
- White space below form feels empty on taller screens

---

### 3. Sign Up Screen

**File:** `ui/auth/SignUpScreen.kt`
**Route:** `Screen.SignUp`

**Layout:**
1. **TopAppBar:** "Create Account" title + back arrow
2. **Form (centered vertically, 24dp horizontal padding):**
   - Full Name `OutlinedTextField` with Person icon
   - Email `OutlinedTextField` with Email icon
   - Password `OutlinedTextField` with Lock icon + visibility toggle
   - Confirm Password `OutlinedTextField` with Lock icon (shows error if mismatch)
   - "Create Account" `Button` (full width, 50dp) — disabled until: name not blank, email not blank, password >= 6 chars, passwords match
   - "Already have an account? Sign In" `TextButton`

**Current Problems:**
- Very plain form — no visual hierarchy or branding
- No hero section like login screen — inconsistent design language
- No password strength indicator
- No email validation feedback (only after submit)
- Standard buttons, not PremiumButton
- Could benefit from a stepper/progress indicator since it's "creating" an account

---

### 4. Onboarding Choice Screen

**File:** `ui/onboarding/OnboardingChoiceScreen.kt`
**State:** Shown after first sign-up/login when `isOnboarded = false`

**Layout (centered vertically, 24dp padding):**
1. "Set Up Your Profile" heading (headlineMedium, centered)
2. "Choose how you'd like to get started" subtitle (bodyLarge, onSurfaceVariant)
3. 48dp spacer
4. **Card 1 — Import from Resume:**
   - Clickable Card (2dp elevation)
   - 24dp internal padding
   - Description icon (48dp, primary color)
   - "Import from Resume" title (titleMedium)
   - "Quick setup from your existing resume" body (bodyMedium, onSurfaceVariant)
5. 16dp spacer
6. **Card 2 — Fill Manually:**
   - Same structure as Card 1
   - Edit icon (48dp, secondary color)
   - "Fill Manually" title
   - "Enter your details step by step" body

**Current Problems:**
- Very plain cards — no visual appeal, no illustrations
- Two plain white cards on white background = low contrast
- No visual indication of which option is recommended
- Cards are not visually distinct enough from each other
- No animation or visual delight
- Could use illustrations instead of generic icons

---

### 5. Resume Import Screen

**File:** `ui/resume/ResumeImportScreen.kt`
**Route:** `Screen.ResumeImport`

**States (AnimatedContent transitions):**

**5a. Idle State:**
- Centered Card (4dp elevation, 32dp padding):
  - CloudUpload icon (64dp, primary)
  - "Import Your Resume" heading (headlineSmall, SemiBold)
  - "Upload a PDF or DOCX file and AI will extract your skills, experience, education, and more." body
  - PremiumButton "Choose File" with Description icon
  - "Supported: PDF, DOCX" label (labelSmall)

**5b. Extracting State:**
- Centered PremiumLoadingIndicator with "Extracting text from file..."

**5c. Parsing State:**
- Centered PremiumLoadingIndicator with "AI is analyzing your resume..."

**5d. Preview State (scrollable):**
- "Review Extracted Data" title
- "Uncheck items you don't want to import" subtitle
- **Personal Info Section** — SectionCard with checkbox + name/email/phone/location/LinkedIn/summary/desired role
- **Skills Section** — count header + list of ItemRow checkboxes (skill name + category/proficiency/years)
- **Experience Section** — count header + ItemRow checkboxes (title at company + dates)
- **Education Section** — count header + ItemRow checkboxes (degree + institution)
- **Certifications Section** — count header + ItemRow checkboxes (name + issuing org)
- **Languages Section** — count header + ItemRow checkboxes (name + proficiency)
- PremiumButton "Import Selected (X items)"

**5e. Importing State:**
- PremiumLoadingIndicator "Importing to your profile..."

**5f. Done State:**
- CheckCircle icon (72dp, primary)
- "Import Complete!" heading
- "Your profile has been updated with the imported data." body
- PremiumButton "Back to Profile"

**5g. Error State:**
- Error icon (64dp, error color)
- "Something went wrong" title
- Error message body
- "Try Again" OutlinedButton

**Current Problems:**
- Preview state is a long plain checklist — no grouping cards, no visual hierarchy
- Checkbox items are cramped — need better spacing and visual treatment
- No progress indicator showing which step (extracting → parsing → preview → importing)
- PremiumButton is used here but inconsistent with rest of app (regular Buttons elsewhere)

---

### 6. Onboarding Screen (Manual Fill)

**File:** `ui/onboarding/OnboardingScreen.kt`
**Steps:** 4 pages in a HorizontalPager (swipe disabled — button-driven)

**Global Elements:**
- LinearProgressIndicator at top (progress = currentPage+1 / 4)
- Animated dots indicator (active dot = 24dp wide primary color, inactive = 8dp wide outlineVariant)
- Bottom navigation row: Back/Skip on left, Next/Finish on right

**Step 1 — Personal Info:**
- "Welcome! Let's set up your profile" heading
- "Tell us about yourself" subtitle
- Fields: Full Name*, Phone, Location, LinkedIn URL, Desired Role
- All OutlinedTextField, full width, single line

**Step 2 — Skills:**
- "Add Your Skills" heading
- "Add skills one at a time. You can always add more later." subtitle
- Fields: Skill Name, Category (programming/tool/soft_skill), Proficiency (beginner/intermediate/advanced/expert)
- "Add Skill" OutlinedButton
- FlowRow of AssistChips showing added skills with name

**Step 3 — Work Experience:**
- "Work Experience" heading
- "Add your most recent position. You can add more later." subtitle
- Fields: Company, Job Title, Start Date (YYYY-MM-DD)
- Listed experiences in Cards (surfaceVariant background) showing title + company + dates

**Step 4 — Professional Summary:**
- "Professional Summary" heading
- "Write a brief summary about yourself. This helps AI generate better content for you." subtitle
- Summary OutlinedTextField (5-10 lines)
- "Finish" button calls markOnboarded

**Current Problems:**
- Text input for categories/proficiency instead of dropdown/chip selection
- Date input as free text (YYYY-MM-DD) instead of date picker
- Can only add ONE work experience before moving on
- No way to add education, certifications, or languages during onboarding
- No illustrations or visual guidance
- Progress dots are nice but steps feel text-heavy and boring
- Skip button placement (first page only) is confusing
- No preview/summary step before finishing

---

### 7. Dashboard Screen (Home Tab)

**File:** `ui/dashboard/DashboardScreen.kt`
**Route:** `Screen.Dashboard`

**Layout:**
1. **TopAppBar:** "Dashboard" title (plain, no styling)
2. **FAB:** ExtendedFloatingActionButton "New Application" with + icon
3. **Content States:**

**7a. Loading State:**
- 4x ShimmerJobCard skeleton placeholders

**7b. Empty State (no jobs):**
- Centered:
  - Empty dashboard illustration (128dp, from `R.drawable.il_empty_dashboard`)
  - "No job applications yet" (titleMedium, onSurfaceVariant)
  - "Tap + to add your first job description" (bodyMedium, onSurfaceVariant)

**7c. Job List:**
- LazyColumn with 8dp vertical spacing, 16dp horizontal padding
- Each item: animated entry (staggered fade + slide up, 50ms delay per item)
- **JobCard:** Material Card (2dp elevation), 16dp padding
  - Left column (weight 1f): Role title (titleMedium, SemiBold), Company name (bodyMedium, onSurfaceVariant), Status text (labelSmall, primary)
  - Right side: Match score percentage (headlineSmall, Bold) — colored green/orange/red based on score
  - Clickable → navigates to JobDetail

**Current Problems:**
- "Dashboard" title is generic — should show greeting or user context
- No stats/summary cards (total applications, average match score, etc.)
- No filtering, sorting, or search
- No status badges (applied, interviewing, rejected, offered)
- Job cards are plain white rectangles — no visual richness
- Match score is just a number — no progress ring or visual indicator
- No swipe actions (delete, mark applied)
- FAB blocks last card — spacer helps but not ideal
- Empty state illustration is an XML vector — likely very basic

---

### 8. Add Job Screen

**File:** `ui/job/AddJobScreen.kt`
**Route:** `Screen.AddJob`

**Layout:**
1. **TopAppBar:** "New Application" + back arrow
2. **Instructions:** "Paste the job description or enter a LinkedIn URL to auto-fetch" (bodyLarge)
3. **URL Input:**
   - OutlinedTextField "Job URL (LinkedIn or other)" with CloudDownload trailing icon
   - "Auto-fetch job details" TextButton (appears when URL not blank)
   - Loading indicator "Fetching job details..." with small spinner
4. **Manual Fields:**
   - Company Name OutlinedTextField
   - Job Title / Role OutlinedTextField
5. **Job Description:**
   - "Job Description" label with "Paste" FilledTonalButton (ContentPaste icon)
   - Large OutlinedTextField (10-20 lines) with placeholder "Paste the full job description here..."
6. **Submit:**
   - "Analyze & Save" Button (full width, 50dp) — disabled until company + role + text filled
   - Shows CircularProgressIndicator when analyzing

**Also supports:** Android Share Intent — when user shares text from another app, it auto-fills rawText

**Current Problems:**
- Very form-heavy — wall of text fields with no visual breaks
- URL auto-fetch feedback is minimal — no success animation
- No character count or length guidance for job description
- Paste button is small and easy to miss
- No drag-and-drop or OCR from screenshot
- "Analyze & Save" gives no indication of what "analyze" means
- No explanation of what AI will do with the data

---

### 9. Job Detail Screen

**File:** `ui/job/JobDetailScreen.kt`
**Route:** `Screen.JobDetail(jobId)`

**Layout (scrollable):**
1. **TopAppBar:** Role title (or "Job Detail") + back arrow
2. **Company & Role:**
   - Company name (titleLarge)
   - Role title (titleMedium, onSurfaceVariant)
3. **Match Score Card:**
   - Background color varies by score (green/orange/red at 0.1 alpha)
   - Score percentage (headlineLarge, Bold, colored) + "Match Score" label
4. **Match Details:**
   - "Matched Skills" section (green) — FlowRow of AssistChips (green tint)
   - "Gaps" section (red) — FlowRow of AssistChips (red tint)
   - "Suggestions" section — bulleted list
5. **Generate Content Section:**
   - "Generate Content" heading (titleLarge)
   - **Tone Selector:** 3 AssistChips — Professional / Casual / Enthusiastic (selected gets primaryContainer background)
   - **Content Buttons Row:**
     - "Cover Letter" Button (Description icon) — weight 1f
     - "Cover Email" Button (Email icon) — weight 1f
   - **Quick Questions:**
     - "Answer Questions" label
     - OutlinedButton: "Why do you want to work here?"
     - OutlinedButton: "What are your strengths?"
     - OutlinedButton: "Tell me about yourself"
   - **Custom Question:**
     - OutlinedTextField (2 lines)
     - "Answer Custom Question" OutlinedButton
6. **Loading:** CircularProgressIndicator (animated visibility)
7. **Generated Content Card:**
   - SecondaryContainer background
   - "Generated Content" title + Copy button
   - Full text display
8. **Saved Content:**
   - "Saved Content" heading
   - List of Cards showing content type + truncated text (200 chars) + Copy button

**Current Problems:**
- This is the richest screen but also the most cluttered
- Match score card is underwhelming — should be a prominent visual (circular progress, animation)
- Skills chips (matched vs gaps) look identical except for color — no icons or visual distinction
- Content generation buttons are cramped and inconsistent (Button vs OutlinedButton)
- Tone selector chips look like regular chips — no clear selected state
- Generated content appears below fold — user might not scroll to see it
- No way to regenerate with different parameters without scrolling back up
- Saved content cards are plain — no way to favorite, share, or delete
- No tab or section navigation for this long screen
- Copy feedback is haptic only — no visual confirmation

---

### 10. Profile Screen (Profile Tab)

**File:** `ui/profile/ProfileScreen.kt`
**Route:** `Screen.Profile`

**Layout (scrollable):**
1. **TopAppBar:** "Profile" title + Edit/Save icon button (toggles edit mode)
2. **Import Resume Card:**
   - PrimaryContainer background
   - + icon + "Import from Resume" title + "Upload PDF or DOCX — AI extracts your data" subtitle
   - Clickable → navigates to ResumeImport
3. **Personal Information Card (surfaceVariant):**
   - "Personal Information" title
   - View mode: ProfileField rows (label: value) — Name, Email, Phone, Location, LinkedIn, Desired Role, Summary
   - Edit mode: OutlinedTextFields for all fields
4. **Skills Card (surfaceVariant):**
   - "Skills" title + Add button
   - FlowRow of AssistChips showing skill name + proficiency + years
   - Edit mode: each chip gets a delete button (red trash icon)
   - Empty state: "No skills added yet"
5. **Work Experience Card (surfaceVariant):**
   - "Work Experience" title
   - List: title (bodyLarge) + company + dates (bodyMedium)
   - Edit mode: delete button per item
   - Empty state: "No work experience added yet"
6. **Education Card (surfaceVariant):**
   - Same pattern as Work Experience
   - Shows degree + institution + field of study
7. **Languages Card (surfaceVariant):**
   - FlowRow of AssistChips with name + proficiency
   - Empty state: "No languages added yet"
8. **Log Out TextButton (red, full width)**
9. **Add Skill Dialog:**
   - AlertDialog with: Skill Name, Category, Proficiency, Years of Experience
   - Add / Cancel buttons

**Current Problems:**
- All cards look identical (surfaceVariant background) — no visual hierarchy
- Profile has no avatar/photo
- No header/banner section with user's name prominently displayed
- Edit mode is toggle-based — no inline editing or clear mode indication
- Add Skill dialog uses free text for category and proficiency — should be dropdowns
- No way to add work experience, education, or languages from profile screen (only during onboarding or resume import)
- No profile completeness indicator (e.g., "Profile 60% complete")
- Log Out button at bottom feels hidden and unimportant
- Import Resume card is functional but doesn't stand out enough
- Spacing between sections is uniform — no visual rhythm

---

### 11. Snippets Screen (Snippets Tab)

**File:** `ui/snippets/SnippetsScreen.kt`
**Route:** `Screen.Snippets`

**Layout:**
1. **TopAppBar:** "Quick Copy" title
2. **FAB:** FloatingActionButton with BubbleChart icon — launches bubble overlay service
3. **Empty State:**
   - Empty snippets illustration (128dp)
   - "No snippets yet"
   - "Add profile data to generate quick-copy snippets"
4. **Content (scrollable):**
   - "Tap any chip to copy its value" instruction (bodyMedium)
   - **Grouped sections** by category (personal, skills, experience entries, education, languages, certifications, summary):
     - Category title (titleMedium) + "Copy All Skills" button (for skills section only)
     - FlowRow of SuggestionChips (secondaryContainer background):
       - ContentCopy icon (16dp) + label text
       - On tap: copies value to clipboard, haptic feedback, snackbar confirmation

**Snippet Categories Generated:**
- Personal: Full Name, Email, Phone, Location, LinkedIn, Desired Role, Portfolio, Salary Expectation
- Skills: each skill as individual chip
- Per-experience: "Role & Period", "Description", "Achievements" (grouped by job title)
- Education: each degree
- Languages: each language
- Certifications: each cert
- Summary: Professional Summary

**Current Problems:**
- Chips are all same color (secondaryContainer) — no visual differentiation by category
- "Copy All Skills" feature exists only for skills — should be available for all categories
- No search/filter for snippets
- Long snippets (like summary or achievements) are hard to preview from chip label alone
- FAB for launching bubble is confusing — no explanation of what it does
- No visual indication that bubble is currently active
- Chips are dense and can be overwhelming with lots of profile data

---

### 12. AI Suggestions Screen (AI Tab)

**File:** `ui/suggestions/AiSuggestionsScreen.kt`
**Route:** `Screen.AiSuggestions`

**States:**

**12a. Loading:**
- Centered: CircularProgressIndicator + "Analyzing your profile..." text

**12b. Error:**
- Centered: error message in error color

**12c. Empty:**
- Centered: AutoAwesome icon + "Complete your profile to get AI suggestions"

**12d. Success (LazyColumn, 16dp padding, 16dp spacing):**
- **Headline Ideas Section:**
  - SuggestionSection card (surface, 1dp elevation) titled "Headline Ideas" with TrendingUp icon
  - Cards with primaryContainer background showing each headline suggestion
- **Summary Improvements Section:**
  - "Summary Improvements" with AutoAwesome icon
  - Numbered option cards (surfaceVariant) each showing rewritten summary
- **Skill Gaps Section:**
  - "Skill Gaps" with Lightbulb icon
  - FlowRow of AssistChips colored by priority:
    - High priority: error color
    - Medium: tertiary color
    - Low: onSurfaceVariant
  - Each chip shows skill name (bold) + reason text (labelSmall)
- **Tips Section:**
  - "Tips" heading
  - Bulleted text list of general tips

**Data Model (AiSuggestionsResponse):**
- `headlineSuggestions: List<String>`
- `summaryRewrites: List<String>`
- `skillGaps: List<SkillGap>` (skill, reason, priority)
- `generalTips: List<String>`

**Caching:** Suggestions cached in Supabase, loaded from cache on screen open. Refresh button fetches new from AI.

**Current Problems:**
- Loading state uses generic CircularProgressIndicator (not PremiumLoadingIndicator)
- No pull-to-refresh
- Skill gap chips try to show too much info in a small space — hard to read
- No "apply suggestion" actions (e.g., tap headline → update profile)
- No way to dismiss/hide suggestions already applied
- SuggestionSection cards all look the same — no visual variety
- No "last updated" timestamp
- Empty state doesn't link to profile screen

---

### 13. Bubble Overlay (Floating Widget)

**File:** `service/BubbleOverlayService.kt`
**Type:** Foreground Service with `TYPE_APPLICATION_OVERLAY`

**Collapsed State (Bubble):**
- 58dp circular floating bubble
- LinkedIn blue (#0A66C2) background with 2.5dp white border
- User's first initial displayed (22sp, Bold, white)
- 12dp shadow
- Draggable with touch handling — snaps to nearest screen edge on release
- Tap toggles expanded panel

**Expanded State (Panel):**
- 92% screen width, 70% screen height
- 16dp rounded corners, surface background, 16dp shadow
- Header: "Quick Copy" title + Close button
- Scrollable content area:
  - Grouped by category (same as snippets screen)
  - Category labels in primary color (labelLarge)
  - FlowRow of SuggestionChips (primaryContainer background)
  - Each chip: 12sp label + 14dp ContentCopy icon
  - Tap → copies to clipboard + Toast notification

**Notification:**
- "JobApplier Bubble Active" with "Tap to open app" + "Stop" action

**Current Problems:**
- Bubble is a plain circle with a letter — not branded enough
- No animation when expanding/collapsing
- Expanded panel has no search — hard to find specific snippet with many items
- Toast for copy feedback is basic — should match app's snackbar style
- No way to resize or reposition the expanded panel
- Panel covers too much screen (70% height) — could be a bottom sheet style
- No indication of which category is open in panel
- Physics snap animation works but no bounce or spring effect

---

## Data Models

### Profile
```
- id: String
- fullName: String
- email: String?
- phone: String?
- location: String?
- linkedinUrl: String?
- portfolioUrl: String?
- summary: String?
- desiredRole: String?
- desiredSalaryMin: Int?
- desiredSalaryMax: Int?
- salaryCurrency: String (default "USD")
- isOnboarded: Boolean
```

### Skill
```
- id: String
- name: String
- category: String? (programming, tool, soft_skill)
- proficiency: String? (beginner, intermediate, advanced, expert)
- yearsExperience: Int?
```

### WorkExperience
```
- id: String
- company: String
- title: String
- location: String?
- startDate: String?
- endDate: String?
- isCurrent: Boolean
- description: String?
- achievements: List<String>
- technologiesUsed: List<String>
- sortOrder: Int
```

### Education
```
- id: String
- institution: String
- degree: String
- fieldOfStudy: String?
- startDate: String?
- endDate: String?
- gpa: String?
- description: String?
```

### Certification
```
- id: String
- name: String
- issuingOrg: String
- issueDate: String?
- expiryDate: String?
- credentialUrl: String?
```

### Language
```
- id: String
- name: String
- proficiency: String?
```

### JobDescription
```
- id: String
- companyName: String
- roleTitle: String
- rawText: String
- sourceUrl: String?
- status: String (default "draft")
- matchScore: Int?
- matchResult: MatchResult?
- notes: String?
- appliedAt: String?
- createdAt: String?
```

### MatchResult
```
- requirements: List<String>
- matchScore: Int
- matched: List<String>
- gaps: List<String>
- partial: List<String>
- suggestions: List<String>
```

### GeneratedContent
```
- id: String
- jobId: String
- contentType: String (cover_letter, cover_email, why_work_here, strengths, motivation, custom_question)
- content: String
- tone: String (professional, casual, enthusiastic)
- version: Int
- isFavorite: Boolean
```

---

## Current Design Problems & Pain Points

### Global Issues
1. **No brand identity** — app looks like a generic Material 3 template, not a sellable SaaS product
2. **Inconsistent component usage** — PremiumButton, GlassmorphismCard, PremiumLoadingIndicator exist but are barely used. Most screens use plain Material defaults
3. **LinkedIn clone aesthetic** — blue-only palette with warm gray creates a dated corporate feel
4. **No illustrations or micro-animations** — screens are text-heavy with no visual delight
5. **No dark mode polish** — dark theme exists but is auto-generated, not carefully designed
6. **No onboarding tutorial or feature discovery** — users are dropped into forms immediately
7. **No empty state engagement** — empty states have basic text, no call-to-action buttons or guides
8. **Bottom nav is unstyled** — default Material NavigationBar with no customization
9. **No profile picture/avatar anywhere** — even though user has profile data
10. **No settings screen** — no way to change theme, notification preferences, or account settings

### UX Flow Issues
1. **Onboarding is too long and tedious** — 4 steps of text fields with no visual reward
2. **Profile editing is clunky** — global edit toggle instead of inline editing per section
3. **Job detail screen is a long scroll** — needs tabs or collapsible sections
4. **No way to track application status changes** — status exists in data model but no UI to update it
5. **No search or filter on dashboard** — unusable with many applications
6. **Snippets screen has no organization** — flat list of chips becomes overwhelming
7. **AI suggestions are not actionable** — no "apply this" buttons

### Visual Issues
1. **Cards are flat and repetitive** — all surfaceVariant, same shape, same elevation
2. **No gradients or depth** — except splash screen, everything is flat solid colors
3. **Typography has no hierarchy** — headings and body text don't have enough contrast
4. **Color-coding is minimal** — match scores use color but nothing else does
5. **No micro-interactions** — only the dashboard has staggered list animation
6. **Buttons are inconsistent** — mix of Button, OutlinedButton, TextButton, FilledTonalButton, PremiumButton with no clear pattern
7. **Loading states inconsistent** — some screens use shimmer, some use spinner, some use PremiumLoadingIndicator

---

## Design Enhancement Goals

When redesigning, the AI agent should aim for:

### Brand & Visual Identity
- Create a distinctive color palette — move beyond LinkedIn blue to a unique, vibrant SaaS palette
- Design a proper logo/wordmark (replace Canvas-drawn "J")
- Add gradient accents throughout (not just splash)
- Create depth with layered cards, shadows, and glassmorphism
- Add illustrations for empty states and onboarding
- Design custom icons or icon style

### Premium SaaS Feel
- Replace all plain Buttons with PremiumButton (or enhanced version)
- Use GlassmorphismCard consistently for important surfaces
- Add micro-animations and transitions throughout
- Create a polished dark mode (not just inverted colors)
- Add subtle background textures or patterns
- Design a proper splash/loading experience

### Component Consistency
- Unify all loading states with PremiumLoadingIndicator
- Use shimmer consistently for list loading
- Create a consistent card hierarchy (primary, secondary, tertiary cards)
- Standardize button styles (primary CTA, secondary, tertiary)
- Design a proper chip/tag system with category colors

### UX Improvements
- Add profile completeness indicator with gamification
- Add stats cards to dashboard (total apps, avg score, weekly trend)
- Add pull-to-refresh everywhere
- Add swipe actions on list items
- Add search and filter capabilities
- Make AI suggestions actionable with one-tap apply
- Add proper date pickers and dropdown selectors
- Add status pipeline/kanban view for job applications
- Add profile avatar/photo support
- Add a settings screen

### Screen-Specific Priorities
1. **Dashboard** — needs stats cards, better job cards with company logos/colors, filter bar
2. **Job Detail** — needs tabbed layout, circular match score indicator, collapsible sections
3. **Profile** — needs header with avatar, completeness bar, inline editing
4. **Login/Signup** — needs social login buttons, better hero section, illustrations
5. **Onboarding** — needs illustrations per step, chip selectors instead of text inputs, progress celebration
6. **Snippets** — needs categorized tabs/sections, better visual grouping, search
7. **AI Suggestions** — needs actionable cards with "apply" buttons, visual variety
8. **Bubble** — needs branded design, smooth animations, compact mode option

---

## File Structure Reference

```
app/src/main/java/com/aplicator/jobapplier/
├── JobApplierApp.kt                    # Hilt Application class
├── MainActivity.kt                     # Single Activity, handles share intents
├── data/
│   ├── event/
│   │   ├── ProfileRefreshTrigger.kt    # Event bus for profile refresh
│   │   └── SharedJobTextHolder.kt      # Holds shared text from intents
│   ├── remote/
│   │   ├── ai/                         # AI request/response DTOs
│   │   └── dto/                        # Supabase DTOs
│   └── repository/                     # Repository implementations
│       ├── AiRepository.kt             # AI proxy calls
│       ├── AuthRepository.kt           # Supabase Auth
│       ├── JobRepository.kt            # Job CRUD
│       ├── ProfileRepository.kt        # Profile CRUD
│       └── ResumeImportRepository.kt   # Resume parsing
├── di/
│   ├── AppModule.kt                    # Supabase client, singletons
│   └── RepositoryModule.kt             # Repository bindings
├── domain/model/
│   ├── JobDescription.kt              # Job, MatchResult, GeneratedContent, UserProfileSnapshot
│   └── Profile.kt                     # Profile, Skill, WorkExperience, Education, Certification, Language
├── service/
│   ├── BubbleDataProvider.kt           # Snippet data for bubble service
│   ├── BubbleOverlayService.kt         # Floating overlay service
│   └── BubblePhysics.kt               # Edge-snap physics engine
└── ui/
    ├── auth/
    │   ├── AuthViewModel.kt
    │   ├── LoginScreen.kt
    │   └── SignUpScreen.kt
    ├── components/
    │   ├── AnimatedGradientBackground.kt
    │   ├── AnimatedSplashContent.kt
    │   ├── GlassmorphismCard.kt         # UNUSED — should be integrated
    │   ├── OverlayPermissionDialog.kt
    │   ├── PremiumButton.kt             # UNDERUSED — only in ResumeImport
    │   ├── PremiumLoadingIndicator.kt   # UNDERUSED — only in ResumeImport
    │   └── ShimmerEffect.kt
    ├── dashboard/
    │   └── DashboardScreen.kt
    ├── job/
    │   ├── AddJobScreen.kt
    │   ├── JobDetailScreen.kt
    │   └── JobViewModel.kt
    ├── navigation/
    │   ├── NavGraph.kt                  # Auth + Main nav graphs + bottom nav
    │   └── Screen.kt                    # Route definitions
    ├── onboarding/
    │   ├── OnboardingChoiceScreen.kt
    │   └── OnboardingScreen.kt
    ├── profile/
    │   ├── ProfileScreen.kt
    │   └── ProfileViewModel.kt
    ├── resume/
    │   ├── ResumeImportScreen.kt
    │   └── ResumeImportViewModel.kt
    ├── snippets/
    │   └── SnippetsScreen.kt
    ├── suggestions/
    │   ├── AiSuggestionsScreen.kt
    │   └── AiSuggestionsViewModel.kt
    └── theme/
        ├── Color.kt                    # Full color palette
        ├── Theme.kt                    # Light + Dark color schemes
        └── Type.kt                     # Plus Jakarta Sans typography
```

---

## Permissions

| Permission | Purpose |
|-----------|---------|
| `INTERNET` | Supabase API calls |
| `SYSTEM_ALERT_WINDOW` | Floating bubble overlay |
| `FOREGROUND_SERVICE` | Bubble overlay foreground service |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Overlay service type |
| `POST_NOTIFICATIONS` | Service notification |

---

## Key Features Summary for Redesign Agent

| Feature | Current State | Enhancement Priority |
|---------|--------------|---------------------|
| Auth (Login/Signup) | Basic forms, no social login | HIGH |
| Onboarding | 4-step text forms | HIGH |
| Resume Import | Good flow, uses PremiumButton | MEDIUM |
| Dashboard | Plain list, no stats | HIGH |
| Job Detail | Feature-rich but cluttered | HIGH |
| Profile | Flat cards, no avatar | HIGH |
| Snippets | Dense chips, no grouping | MEDIUM |
| AI Suggestions | Functional but not actionable | MEDIUM |
| Bubble Overlay | Works but plain | LOW |
| Theme/Colors | LinkedIn clone, monotone blue | CRITICAL |
| Component Library | Exists but underutilized | CRITICAL |
| Dark Mode | Auto-generated, unpolished | MEDIUM |
| Animations | Minimal | HIGH |
| Empty States | Basic text | MEDIUM |
