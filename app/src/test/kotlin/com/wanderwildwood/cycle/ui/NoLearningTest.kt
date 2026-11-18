package com.wanderwildwood.cycle.ui

import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class NoLearningTest {
    @Test fun learningIsTurnedOffAndTheRestKept() {
        val options = noLearning(EditorInfo.IME_ACTION_DONE)
        assertEquals(EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING, options and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)
        assertEquals(EditorInfo.IME_ACTION_DONE, options and EditorInfo.IME_MASK_ACTION)
    }
}
