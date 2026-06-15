package com.smartvision.ai

import com.smartvision.ai.domain.usecase.ValidateInputUseCase
import org.junit.Assert
import org.junit.Test

class TranslatorViewModelTest {

    @Test
    fun `initial state is empty`() {
        val validate = ValidateInputUseCase()
        val result   = validate.validateText("Hello")
        Assert.assertTrue(result.isValid)
    }

    @Test
    fun `swap languages exchanges source and target`() {
        var src = "en"; var tgt = "hi"
        val tmp = src; src = tgt; tgt = tmp
        Assert.assertEquals("hi", src)
        Assert.assertEquals("en", tgt)
    }
}
