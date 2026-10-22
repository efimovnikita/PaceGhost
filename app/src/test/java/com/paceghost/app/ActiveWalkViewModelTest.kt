package com.paceghost.app

import android.app.Application
import com.paceghost.app.service.geocode.RouteGeocoder
import com.paceghost.app.ui.active.ActiveWalkViewModel
import com.paceghost.app.ui.home.HomeViewModel
import com.paceghost.app.ui.preview.TrackPreviewViewModel
import org.junit.Assert.assertNotNull
import org.junit.Test

class ActiveWalkViewModelTest {

    @Test
    fun `ActiveWalkViewModel must have single Application constructor for ViewModelProvider`() {
        val constructor = ActiveWalkViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull(constructor)
    }

    @Test
    fun `ActiveWalkViewModel must have two-argument constructor for dependency injection`() {
        val constructor = ActiveWalkViewModel::class.java.getConstructor(
            Application::class.java,
            RouteGeocoder::class.java
        )
        assertNotNull(constructor)
    }

    @Test
    fun `All app ViewModels must have single Application constructor for Jetpack Compose viewModel()`() {
        assertNotNull(HomeViewModel::class.java.getConstructor(Application::class.java))
        assertNotNull(TrackPreviewViewModel::class.java.getConstructor(Application::class.java))
        assertNotNull(ActiveWalkViewModel::class.java.getConstructor(Application::class.java))
    }
}
