package com.github.fragivity

import android.os.Bundle
import android.os.Parcelable
import androidx.collection.SparseArrayCompat
import androidx.collection.valueIterator
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.FragmentNavigator
import kotlinx.android.parcel.Parcelize

private const val NAV_DEST_NODES_KEY = "NavDestKey"

internal class MyViewModel(
    //fix https://github.com/vitaviva/fragivity/issues/9
    private val _handle: SavedStateHandle) : ViewModel() {

    lateinit var navController: NavController

    val nodes by lazy {
        val list: List<NavDestinationBundle> =
            _handle.get<Bundle>(NAV_DEST_NODES_KEY)?.getParcelableArrayList(null) ?: emptyList()

        val nodes = SparseArrayCompat<NavDestination>()
        list.forEach {
            val clazz = Class.forName(it.className) as Class<Fragment>
            val destination = navController.createNavDestination(clazz.hashCode(), clazz.kotlin)
            nodes.put(destination.id, destination)
        }
        nodes
    }

    init {
        /**
         *  当杀进程重启（例如不保留活动等）时，NavGraph中的nodes信息也需要重建，
         *  所以需要借助SavedStateHandle对ViewModel数据持久化
         */
        _handle.setSavedStateProvider(NAV_DEST_NODES_KEY) {
            val list = ArrayList<NavDestinationBundle>().apply {
                nodes.valueIterator().forEach { add(NavDestinationBundle((it))) }
            }
            Bundle().apply { putParcelableArrayList(null, list) }
        }
    }

}

@Parcelize
internal data class NavDestinationBundle(val className: String) : Parcelable {
    companion object {
        operator fun invoke(destination: NavDestination) =
            NavDestinationBundle((destination as FragmentNavigator.Destination).className)
    }
}