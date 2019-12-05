package jp.techacademy.kinugawa.mikako.qa_app

import android.app.Application
import java.util.ArrayList

//Applicationクラス アプリを起動させるときに一番最初に通るクラス
//起動したときに、まずお気に入りを保持しているか確認したいので実装している

class favorite(val fav: String): Application() {

}