package com.creditdb.pro

object AppConfig {
    const val GITHUB_OWNER = "nanbeimerco"
    const val GITHUB_REPO = "CreditDB-Pro-Mobile"
    const val DB_ASSET_NAME = "creditdb.db"

    // 新しい順の Release 一覧。DB を含む最新のものを選ぶ（APK だけの Release が最新でも DB 更新が壊れないように）
    val RELEASES_API_URL = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases?per_page=30"
}
