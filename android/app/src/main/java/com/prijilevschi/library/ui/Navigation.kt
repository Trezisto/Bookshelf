package com.prijilevschi.library.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.prijilevschi.library.AppContainer
import com.prijilevschi.library.LibraryApplication
import com.prijilevschi.library.ui.book.BookDetailScreen
import com.prijilevschi.library.ui.book.BookEditScreen
import com.prijilevschi.library.ui.library.LibraryScreen
import com.prijilevschi.library.ui.settings.SettingsScreen
import com.prijilevschi.library.ui.shelves.ShelvesScreen
import kotlinx.serialization.Serializable

@Serializable
object LibraryRoute

@Serializable
data class BookRoute(val id: Long)

/** [id] = -1 adds a new book. */
@Serializable
data class EditBookRoute(val id: Long = -1)

@Serializable
object ShelvesRoute

@Serializable
object SettingsRoute

/** A ViewModel scoped to the current destination, built from the app's [AppContainer]. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(crossinline create: (AppContainer) -> VM): VM {
    val container = (LocalContext.current.applicationContext as LibraryApplication).container
    return viewModel(factory = viewModelFactory { initializer { create(container) } })
}

@Composable
fun LibraryNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = LibraryRoute) {
        composable<LibraryRoute> {
            LibraryScreen(
                onBookClick = { nav.navigate(BookRoute(it)) },
                onAddBook = { nav.navigate(EditBookRoute()) },
                onShelves = { nav.navigate(ShelvesRoute) },
                onSettings = { nav.navigate(SettingsRoute) },
            )
        }
        composable<BookRoute> { entry ->
            val id = entry.toRoute<BookRoute>().id
            BookDetailScreen(
                bookId = id,
                onBack = { nav.popBackStack() },
                onEdit = { nav.navigate(EditBookRoute(id)) },
            )
        }
        composable<EditBookRoute> { entry ->
            val id = entry.toRoute<EditBookRoute>().id
            BookEditScreen(
                bookId = id.takeIf { it >= 0 },
                onDone = { nav.popBackStack() },
                onOpenSettings = { nav.navigate(SettingsRoute) },
            )
        }
        composable<ShelvesRoute> {
            ShelvesScreen(onBack = { nav.popBackStack() })
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
