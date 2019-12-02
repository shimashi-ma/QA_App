package jp.techacademy.kinugawa.mikako.qa_app

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ListView

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase


import java.util.HashMap

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference
    private lateinit var mFavoriteRef: DatabaseReference  //お気に入り用

    //お気に入り用リファレンス
    private lateinit var mDataBaseReference: DatabaseReference

    //お気に入り用
    private var mlike = false
    private var mFarorite = ""


    //回答用
    private val mEventListener = object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                    val map = dataSnapshot.value as Map<String, String>

                    val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    //お気に入り用
    private val FaroriteListener = object : ChildEventListener{
        override fun onChildAdded(p0: DataSnapshot, p1: String?) {
            val a = p0.value
            mFarorite = a.toString()
        }
        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras.get("question") as Question

        //UI設定
        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
            }
        }

        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            //ログインしていない場合、お気に入りボタンを非表示にする
            like_fab.hide()

            return

        } else if (user != null && mFarorite != null) {

            like_fab.show()
            Log.d("tttaaaggg",mFarorite.toString())

            //画像を変更
            like_fab.setImageResource(R.drawable.baseline_favorite_black_36)



            //お気に入り登録済かどうかFirebaseを参照
            //mDataBaseReference = FirebaseDatabase.getInstance().reference
            //mDataBaseReference.addValueEventListener( object : ValueEventListener {
            //override fun onDataChange(dataSnapshot: DataSnapshot) {
            //val value = dataSnapshot.getValue
            //}
            //override fun onCancelled(error: DatabaseError) {}


        } else if (user != null && mFarorite == null) {

            like_fab.show()
            Log.d("tttaaaggg","ログインしていてかつお気に入り未登録")

            //画像を変更
            like_fab.setImageResource(R.drawable.baseline_favorite_border_black_36)



        }


        //お気に入りボタンをタップした場合
        like_fab.setOnClickListener {

            if (mlike == false) {

                //スナックバー
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view, "お気に入りに追加しました", Snackbar.LENGTH_SHORT).show()

                //画像を変更
                like_fab.setImageResource(R.drawable.baseline_favorite_black_36)

                //フラグを立てる
                mlike = true

                //お気に入りを保存
                mDataBaseReference = FirebaseDatabase.getInstance().reference
                val genreRef = mDataBaseReference.child(favoritesPATH).child(user!!.uid)
                    .child(mQuestion.questionUid)
                val data = HashMap<String, String>()
                // genre
                data["genre"] = mQuestion.genre.toString() //dateのキーはString型　genreはInt型
                genreRef.setValue(data)

                Log.d("tttaaaggg",mFarorite)

                return@setOnClickListener

            } else if (mlike == true) {

                //スナックバー
                val view = findViewById<View>(android.R.id.content)
                Snackbar.make(view, "お気に入りから削除しました", Snackbar.LENGTH_SHORT).show()

                //画像を変更
                like_fab.setImageResource(R.drawable.baseline_favorite_border_black_36)

                //フラグを落とす
                mlike = false

                //お気に入りを削除
                mDataBaseReference = FirebaseDatabase.getInstance().reference
                val genreRef = mDataBaseReference.child(favoritesPATH).child(user!!.uid)
                    .child(mQuestion.questionUid)
                genreRef.removeValue()

                return@setOnClickListener
            }

        }



        val dataBaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)

        //お気に入り
        mFavoriteRef = dataBaseReference.child(favoritesPATH).child(user!!.uid).child(mQuestion.genre.toString())
        mFavoriteRef.addChildEventListener(FaroriteListener)
    }
}