package jp.techacademy.kinugawa.mikako.qa_app

import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.InputMethodManager

import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_login.*

import java.util.HashMap

class LoginActivity : AppCompatActivity() {

    //lateinit 初期化を後で行うようにしている
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mCreateAccountListener: OnCompleteListener<AuthResult> //アカウント作成用　　処理の完了を受け取るリスナー　OnCompleteListenerクラス
    private lateinit var mLoginListener: OnCompleteListener<AuthResult> //ログイン処理用
    private lateinit var mDataBaseReference: DatabaseReference //データベースへの読み書きに必要なDatabaseReferenceクラス

    // アカウント作成時にフラグを立て、ログイン処理後に名前をFirebaseに保存する
    private var mIsCreateAccount = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //データベースリファレンスのインスタンス取得
        mDataBaseReference = FirebaseDatabase.getInstance().reference

        // FirebaseAuthのオブジェクトを取得する
        mAuth = FirebaseAuth.getInstance()

        // アカウント作成処理のリスナー
        mCreateAccountListener = OnCompleteListener { task ->
            if (task.isSuccessful) {  ////引数で渡ってきたTaskクラスのisSuccessfulメソッドで成功したかどうかを確認している
                // 成功した場合
                // ログインを行う
                val email = emailText.text.toString()
                val password = passwordText.text.toString()
                login(email, password) //下の方にloginメソッド定義してある

            } else {

                // 失敗した場合
                // エラーを表示する
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view, "アカウント作成に失敗しました", Snackbar.LENGTH_LONG).show()

                // プログレスバーを非表示にする
                progressBar.visibility = View.GONE
            }
        }

        // ログイン処理のリスナー
        mLoginListener = OnCompleteListener { task ->
            if (task.isSuccessful) {
                // 成功した場合
                val user = mAuth.currentUser //currentUserプロパティ 現在ログインしているユーザーを取得 ログインしていない場合null
                val userRef = mDataBaseReference.child(UsersPATH).child(user!!.uid)  //usersカテゴリ配下に、現在ログインしているユーザーのuid作成

                if (mIsCreateAccount) {  //もしフラグがtrueだったら
                    // アカウント作成の時は表示名をFirebaseに保存する
                    val name = nameText.text.toString()

                    val data = HashMap<String, String>()  //Firebaseは、データをKeyとValueの組み合わせで保存
                    data["名前"] = name
                    userRef.setValue(data)  //DatabaseReferenceが指し示すKeyにValueを保存するには setValue メソッドを使用

                    // 表示名をPrefarenceに保存する
                    saveName(name)  //下の方にsaveNameメソッド定義してある

                } else {
                    //ログインボタンをタップしたときは、Firebaseから表示名を取得してPreferenceに保存。
                    //Firebaseからデータを一度だけ取得する場合はDatabaseReferenceクラスが実装しているQueryクラスのaddListenerForSingleValueEventメソッドを使う
                    userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val data = snapshot.value as Map<*, *>?
                            saveName(data!!["なまえ"] as String)
                        }

                        override fun onCancelled(firebaseError: DatabaseError) {}   //使わないけど実装はいるってこと？
                    })
                }

                // プログレスバーを非表示にする
                progressBar.visibility = View.GONE

                // finish() メソッドで LoginActivity を閉じる
                finish()

            } else {
                // 失敗した場合
                // エラーを表示する
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view, "ログインに失敗しました", Snackbar.LENGTH_LONG).show()

                // プログレスバーを非表示にする
                progressBar.visibility = View.GONE
            }
        }

        // UIの準備
        title = "ログイン"

        //アカウント作成ボタンがタップされた時
        createButton.setOnClickListener { v ->
            // キーボードが出てたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            val email = emailText.text.toString()
            val password = passwordText.text.toString()
            val name = nameText.text.toString()

            if (email.length != 0 && password.length >= 6 && name.length != 0) {  //lengthメソッドは文字列の長さを取得するメソッド

                // ログイン時に表示名を保存するようにフラグを立てる
                mIsCreateAccount = true

                //アカウント作成処理を開始
                createAccount(email, password)

            } else {
                // エラーを表示する
                Snackbar.make(v, "正しく入力してください", Snackbar.LENGTH_LONG).show()
            }
        }

        //ログインボタンがタップされた時
        loginButton.setOnClickListener { v ->
            // キーボードが出てたら閉じる
            val im = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            im.hideSoftInputFromWindow(v.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)

            val email = emailText.text.toString()
            val password = passwordText.text.toString()

            if (email.length != 0 && password.length >= 6) {
                // フラグを落としておく
                mIsCreateAccount = false

                login(email, password)
            } else {
                // エラーを表示する
                Snackbar.make(v, "正しく入力してください", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    //アカウント作成メソッド
    private fun createAccount(email: String, password: String) {
        // プログレスバーを表示する
        progressBar.visibility = View.VISIBLE

        // アカウントを作成する　リスナーの設定もする
        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(mCreateAccountListener)
    }

    //ログインメソッド
    private fun login(email: String, password: String) {
        // プログレスバーを表示する
        progressBar.visibility = View.VISIBLE

        // ログインする　リスナーの設定もする
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(mLoginListener)
    }

    //表示名（なまえ）をリファレンスに保存するメソッド
    private fun saveName(name: String) {
        // Preferenceに保存する
        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        val editor = sp.edit()
        editor.putString(NameKEY, name)
        editor.commit()  //忘れずにcommitメソッドを呼び出して保存処理を反映
    }
}

