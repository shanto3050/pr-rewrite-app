package com.rewriteai.data

enum class RewriteStyle(val displayName: String, val apiValue: String) {
    SUPER_CASUAL("超カジュアル（友だち）", "SUPER_CASUAL"),
    CASUAL_POLITE("カジュアル丁寧（先輩・同僚）", "CASUAL_POLITE"),
    POLITE("ビジネス丁寧（上司）", "POLITE"),
    VERY_FORMAL("超フォーマル（顧客・役員）", "VERY_FORMAL");
}
