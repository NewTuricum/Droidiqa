package ch.newturicum.droidiqa.util

import androidx.lifecycle.LifecycleOwner

/**
 * Let your application class implement this in order for Droidiqa to automatically manage app back- & foreground changes.
 */
interface DroidiqaLifecycleProvider {
    /**
     * Provide your app's process lifecycle here.
     * @see <a href="https://developer.android.com/reference/androidx/lifecycle/ProcessLifecycleOwner">ProcessLifecycleOwner</a>
     */
    fun getProcessLifecycleOwner(): LifecycleOwner
}