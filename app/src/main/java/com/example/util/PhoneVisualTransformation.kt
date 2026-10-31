package com.example.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class PhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 10) text.text.substring(0, 10) else text.text
        val builder = AnnotatedString.Builder()
        
        for (i in trimmed.indices) {
            if (i == 3 || i == 6 || i == 8) {
                builder.append(" ")
            }
            builder.append(trimmed[i])
        }
        
        val transformedText = builder.toAnnotatedString()
        
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 3) return offset
                if (offset <= 6) return offset + 1
                if (offset <= 8) return offset + 2
                return offset + 3
            }
            
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 3) return offset
                if (offset <= 7) return offset - 1
                if (offset <= 10) return offset - 2
                return offset - 3
            }
        }
        
        return TransformedText(transformedText, offsetMapping)
    }
}
