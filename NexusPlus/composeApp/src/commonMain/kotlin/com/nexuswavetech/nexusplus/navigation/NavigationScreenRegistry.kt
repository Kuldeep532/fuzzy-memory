package com.nexuswavetech.nexusplus.navigation

import androidx.compose.runtime.Composable

/**
 * NavigationScreenRegistry — commonMain screen registry for KMP navigation.
 *
 * Each platform (androidMain, iosMain, desktopMain) registers its screen
 * factory via [registerScreen]. The commonMain [NexusNavHost] then uses
 * these factories to build the navigation graph.
 *
 * This enables cross-platform navigation without hardcoding platform-specific
 * screen imports in commonMain.
 */
object NavigationScreenRegistry {

    private val factories = mutableMapOf<String, @Composable (ScreenFactoryContext) -> Unit>()

    /** Register a screen composable for a given route. */
    fun registerScreen(
        route: String,
        factory: @Composable (ScreenFactoryContext) -> Unit,
    ) {
        factories[route] = factory
    }

    /** Get the factory for a route, or null if not registered. */
    fun getFactory(route: String): (@Composable (ScreenFactoryContext) -> Unit)? = factories[route]

    /** Check if a route has a registered factory. */
    fun hasScreen(route: String): Boolean = route in factories

    /** All registered routes. */
    val registeredRoutes: Set<String> get() = factories.keys.toSet()

    /** Clear all registrations (for testing). */
    fun clear() {
        factories.clear()
    }
}

/**
 * Context passed to screen factories. Contains navigation callbacks
 * that are safe to use across platforms.
 */
interface ScreenFactoryContext {
    /** Navigate to a route. */
    fun navigate(route: String)
    /** Pop the back stack. */
    fun popBackStack()
    /** Navigate to Welcome, clearing all back stack. */
    fun navigateToWelcome()
    /** Navigate to a screen with a string argument. */
    fun navigateWithArgument(route: String, argument: String)
}

/** Default implementation of ScreenFactoryContext. */
class DefaultScreenFactoryContext(
    private val navigateFn: (String) -> Unit,
    private val popBackStackFn: () -> Unit,
    private val navigateToWelcomeFn: () -> Unit,
    private val navigateWithArgumentFn: (String, String) -> Unit = { r, a -> navigateFn("$r/$a") },
) : ScreenFactoryContext {
    override fun navigate(route: String) = navigateFn(route)
    override fun popBackStack() = popBackStackFn()
    override fun navigateToWelcome() = navigateToWelcomeFn()
    override fun navigateWithArgument(route: String, argument: String) = navigateWithArgumentFn(route, argument)
}
