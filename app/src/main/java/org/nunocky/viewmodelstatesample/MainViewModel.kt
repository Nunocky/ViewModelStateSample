package org.nunocky.viewmodelstatesample

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import java.io.File
import java.io.FileOutputStream

private const val STATE_KEY_IMAGE = "image"
private const val STATE_KEY_TEXT = "text"
private const val KEY_FILENAME = "filename"

// 実験
// USE_SAVED_STATEの true / falseを変更して挙動の違いを見る
//  - ボタンをクリックしてテキストと画像を変更
//  - ホームボタンを押してアプリを一旦閉じる
//  - 再びアプリを起動する

// Saved Stateを使用するときは trueに
private const val USE_SAVED_STATE = true

/**
 *
 */
class MainViewModel(
    private val app: Application,
    private val savedStateHandle: SavedStateHandle
) : AndroidViewModel(app) {

    // Activity破棄時に保存する画像のファイル名
    private var tempBitmapFile: String? = null

    init {
        // SavedStateに保存された一時ファイルがあればそこから Bitmapを復元する。
        // initから直接 LiveDataにアクセスできない(まだ初期化されていない)ので一度ファイル名を記憶する
        val bundle = savedStateHandle.get<Bundle>(STATE_KEY_IMAGE)
        if (bundle != null) {
            bundle.getString(KEY_FILENAME)?.let {
                tempBitmapFile = it
            }
        }

        // Bitmapを一時ファイルに保存し、SavedStateにはそのファイル名を保存する。
        savedStateHandle.setSavedStateProvider(STATE_KEY_IMAGE) {
            if (image.value != null) {
                val filename = app.saveTempBitmap(image.value!!)
                Bundle().apply {
                    putString(KEY_FILENAME, filename)
                }
            } else {
                Bundle()
            }
        }
    }

    // Stringの SavedState対応
    val text: MutableLiveData<String> =
        if (USE_SAVED_STATE)
            savedStateHandle.getLiveData(STATE_KEY_TEXT, "")
        else
            MutableLiveData("")

    // 非 Parcelableなオブジェクトの Saved State対応 (Bitmapは Parcelableだけど一例として)
    val image: MutableLiveData<Bitmap?> =
        if (USE_SAVED_STATE)
            MutableLiveData<Bitmap?>(restoreBitmap())
        else
            MutableLiveData<Bitmap?>()

    /**
     * ボタンをクリックするたびに表示されるテキストと画像を変更する
     */
    fun setRandomTextAndImage() {
        text.value = arrayOf("Hello World", "HaHaHa", "Blah Blah Blah").random()

        val img = arrayOf(R.drawable.a, R.drawable.b, R.drawable.c).random()
        val bitmap = app.loadBitmap(img)
        image.value = bitmap
    }

    private fun restoreBitmap(): Bitmap? {
        return if (tempBitmapFile != null) {
            val file = File(tempBitmapFile!!)
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            file.delete() // 使い終わったファイルを消す
            return bitmap
        } else null
    }
}

/**
 * drawableからビットマップをロード
 */
private fun Application.loadBitmap(imgResource: Int): Bitmap {
    return BitmapFactory.decodeResource(resources, imgResource)
}

/**
 * ビットマップを一時ファイルに保存
 */
private fun Application.saveTempBitmap(bitmap: Bitmap): String {
    val file = File.createTempFile("image", "", cacheDir)
    FileOutputStream(file).use { fos ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
    }

    return file.absolutePath
}
