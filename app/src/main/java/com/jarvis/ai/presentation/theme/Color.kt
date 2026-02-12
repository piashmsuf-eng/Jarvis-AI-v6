package com.jarvis.ai.presentation.theme

import androidx.compose.ui.graphics.Color

// ── Jarvis AI Theme Colors (Arc Reactor Inspired) ──────────────────────

// Primary Colors - Jarvis Cyan (Arc Reactor Glow)
val PrimaryCyan = Color(0xFF00E5FF)        // Main cyan
val PrimaryCyanDark = Color(0xFF0097A7)    // Darker cyan
val PrimaryCyanLight = Color(0xFF6EFFFF)   // Lighter cyan

// Secondary Colors - Electric Blue
val SecondaryBlue = Color(0xFF2196F3)
val SecondaryBlueDark = Color(0xFF1565C0)
val SecondaryBlueLight = Color(0xFF64B5F6)

// Accent Colors
val AccentPurple = Color(0xFFBB86FC)
val AccentGreen = Color(0xFF03DAC6)
val AccentOrange = Color(0xFFFF9800)

// Background Colors - Dark Theme
val BackgroundDark = Color(0xFF0A0E1A)       // Deep dark blue-black
val SurfaceDark = Color(0xFF151B2E)          // Slightly lighter surface
val SurfaceVariantDark = Color(0xFF1E2538)   // Card backgrounds

// Text Colors
val OnBackgroundLight = Color(0xFFE4E6EB)    // Primary text on dark bg
val OnSurfaceLight = Color(0xFFCFD4E0)       // Secondary text
val OnPrimaryLight = Color(0xFF000000)       // Text on cyan background

// Status Colors
val ErrorRed = Color(0xFFCF6679)
val SuccessGreen = Color(0xFF4CAF50)
val WarningYellow = Color(0xFFFFC107)

// AI Visualization Colors
val VoiceWaveformActive = PrimaryCyan
val VoiceWaveformInactive = Color(0xFF37474F)
val ArcReactorCore = Color(0xFFFFFFFF)
val ArcReactorRing = PrimaryCyan
val ArcReactorGlow = PrimaryCyanLight.copy(alpha = 0.3f)

// Chat Bubble Colors
val BubbleJarvis = SurfaceVariantDark
val BubbleUser = PrimaryCyanDark.copy(alpha = 0.2f)
val BubbleJarvisText = OnSurfaceLight
val BubbleUserText = PrimaryCyanLight

// Alternative Theme Colors (Iron Man Red)
val IronManRed = Color(0xFFE53935)
val IronManGold = Color(0xFFFFD700)

// Alternative Theme Colors (Hulk Green)
val HulkGreen = Color(0xFF4CAF50)
val HulkDark = Color(0xFF1B5E20)

// Alternative Theme Colors (Thanos Purple)
val ThanosPurple = Color(0xFF9C27B0)
val ThanosDark = Color(0xFF4A148C)
