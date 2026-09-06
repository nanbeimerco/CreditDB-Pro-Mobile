package com.creditdb.pro.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null
) {
    fun getLocalizedTitle(isEn: Boolean = com.creditdb.pro.ui.theme.LanguageManager.isEnglish): String {
        return if (isEn) {
            when (this) {
                is Works -> com.creditdb.pro.ui.theme.AppStrings.navWorks
                is Predict -> com.creditdb.pro.ui.theme.AppStrings.navPredict
                is Staff -> com.creditdb.pro.ui.theme.AppStrings.navStaff
                is Guide -> com.creditdb.pro.ui.theme.AppStrings.navGuide
                is WorkDetail -> "Work Details"
                is StaffDetail -> "Staff Details"
                is StudioDetail -> "Studio Details"
            }
        } else {
            title
        }
    }
    object Works : Screen(
        route = "works",
        title = "作品DB",
        selectedIcon = Icons.Filled.Movie,
        unselectedIcon = Icons.Outlined.Movie
    )

    object Predict : Screen(
        route = "predict",
        title = "予測編成",
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome
    )

    object Staff : Screen(
        route = "staff",
        title = "制作・声優",
        selectedIcon = Icons.Filled.EmojiEvents,
        unselectedIcon = Icons.Outlined.EmojiEvents
    )

    object Guide : Screen(
        route = "guide",
        title = "解説",
        selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
        unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook
    )

    object WorkDetail : Screen(
        route = "work_detail/{workId}",
        title = "作品詳細"
    ) {
        fun createRoute(workId: String) = "work_detail/$workId"
    }

    object StaffDetail : Screen(
        route = "staff_detail/{staffName}",
        title = "スタッフ詳細"
    ) {
        fun createRoute(staffName: String) = "staff_detail/${java.net.URLEncoder.encode(staffName, "UTF-8")}"
    }

    object StudioDetail : Screen(
        route = "studio_detail/{studioName}",
        title = "制作スタジオ詳細"
    ) {
        fun createRoute(studioName: String) = "studio_detail/${java.net.URLEncoder.encode(studioName, "UTF-8")}"
    }

    companion object {
        val bottomNavItems = listOf(Works, Predict, Staff)
    }
}
