package com.example

const val MAX_MATERIAL_PHOTOS_PER_CLASS = 5

fun remainingMaterialPhotoSlots(currentCount: Int): Int =
    (MAX_MATERIAL_PHOTOS_PER_CLASS - currentCount).coerceAtLeast(0)
