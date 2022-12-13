package com.example.programmingquotes.core.common

fun createEmojiWithDecimalCode(emojiCode: Int): String = String(Character.toChars(emojiCode))

fun generateRandomEmoji() = (128512..128580).random()