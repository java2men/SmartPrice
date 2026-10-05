package ru.embtlab.smartprice.presentation.navigation

sealed interface Screen {
    val route: String

    data object Compare : Screen {
        override val route = "compare"
    }

    data object Settings : Screen {
        override val route = "settings"
    }
}