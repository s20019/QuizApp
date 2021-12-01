package jp.ac.it_college.std.s20019.quiz2

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        // データベースファイル名
        private const val DATABASE_NAME = "quiz.db"
        // バージョン情報
        private const val DATABASE_VERSION = 1
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // テーブル作成用SQL文字列の作成
        val createTable = """
            CREATE TABLE Quiz (
                _id       INTEGER PRiMARY KEY,
                question  TEXT,
                answer    LONG,
                choiceA   TEXT,
                choiceB   TEXT,
                choiceC   TEXT,
                choiceD   TEXT,
                choiceE   TEXT,
                choiceF   TEXT
            );
        """.trimIndent()
        db?.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, p1: Int, p2: Int) {

    }
}