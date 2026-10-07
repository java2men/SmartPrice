package ru.embtlab.smartprice.presentation.navigation

sealed interface Screen {
    val route: String

    data object Compare : Screen {
        override val route = "compare"
    }

    data object Settings : Screen {
        override val route = "settings"
    }

    data object EditProduct : Screen {
        override val route = "product_edit/{productId}"
        fun createRoute(productId: String): String = "product_edit/$productId"
    }
}