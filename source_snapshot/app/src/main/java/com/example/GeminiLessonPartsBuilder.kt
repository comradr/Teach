package com.example

object GeminiLessonPartsBuilder {

    fun buildParts(
        useSecondClass: Boolean,
        classAImages: List<DraftLessonImage>,
        classBImages: List<DraftLessonImage>,
        userPrompt: String
    ): List<Part> {
        val parts = mutableListOf<Part>(Part(text = userPrompt))
        val classAParts = imageParts(classAImages)
        val classBParts = imageParts(classBImages)

        if (useSecondClass) {
            if (classAParts.isNotEmpty()) {
                parts.add(Part(text = "МАТЕРИАЛЫ КЛАССА 1.\nСледующие ${classAParts.size} фото относятся ТОЛЬКО к Классу 1 и идут в порядке страниц учебника. Рассмотри все изображения и используй задания с них.\nНе используй их для Класса 2."))
                parts.addAll(classAParts)
            }
            if (classBParts.isNotEmpty()) {
                parts.add(Part(text = "МАТЕРИАЛЫ КЛАССА 2.\nСледующие ${classBParts.size} фото относятся ТОЛЬКО к Классу 2 и идут в порядке страниц учебника. Рассмотри все изображения и используй задания с них.\nНе используй их для Класса 1."))
                parts.addAll(classBParts)
            }
        } else {
            if (classAParts.isNotEmpty()) {
                parts.add(Part(text = "МАТЕРИАЛЫ К УРОКУ.\nСледующие ${classAParts.size} фото идут в порядке страниц учебника. Рассмотри все изображения и используй задания с них, если пользователь не указал иное."))
                parts.addAll(classAParts)
            }
        }

        return parts
    }

    private fun imageParts(images: List<DraftLessonImage>): List<Part> = images.mapNotNull { image ->
        ImageOptimizer.getBase64FromPath(image.path)?.let { base64 ->
            Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64))
        }
    }
}
