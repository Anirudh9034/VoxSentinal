# VoxSentinel

Java module for **SIH26104** — AI-based voice cloning / deepfake detection.

**This module (Person C):** Liveness detection using [TarsosDSP](https://github.com/JorenSix/TarsosDSP).
Detects pitch jitter, shimmer, and pause/silence gaps to flag synthetic or replayed audio
that lacks natural human vocal variation.

## Stack
- Java 17
- Maven
- TarsosDSP 2.5

## Status
🔧 In progress — pitch detection pipeline set up, testing with sample audio.
