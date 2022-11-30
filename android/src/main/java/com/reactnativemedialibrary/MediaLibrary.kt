package com.reactnativemedialibrary

import android.provider.MediaStore
import android.content.Context
import android.net.Uri
import android.os.Build
import com.facebook.jni.HybridData
import com.facebook.proguard.annotations.DoNotStrip
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.turbomodule.core.CallInvokerHolderImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject

@DoNotStrip
class MediaLibrary(context: Context) {
  @DoNotStrip
  private var mHybridData: HybridData? = null

  @Suppress("KotlinJniMissingFunction")
  external fun initHybrid(
    jsContext: Long,
    jsCallInvokerHolder: CallInvokerHolderImpl?
  ): HybridData?

  @Suppress("KotlinJniMissingFunction")
  external fun installJSIBindings()
  private val context: Context

  private val job = SupervisorJob()
  val scope = CoroutineScope(Dispatchers.IO + job)

  init {
    this.context = context.applicationContext
  }

  fun install(context: ReactApplicationContext): Boolean {
    System.loadLibrary("react-native-media-library")
    val jsContext = context.javaScriptContextHolder
    val jsCallInvokerHolder = context.catalystInstance.jsCallInvokerHolder
    mHybridData = initHybrid(
      jsContext.get(),
      jsCallInvokerHolder as CallInvokerHolderImpl
    )
    installJSIBindings()
    return true
  }

  @DoNotStrip
  fun getAssets(params: String, callback: GetAssetsCallback) {
    scope.launch {
      val contentResolver = context.contentResolver
      val jsonArray = contentResolver.listQuery(
        EXTERNAL_CONTENT_URI,
        context,
        params.asJsonInput()
      )
      callback.onChange(jsonArray.toString())
    }
  }

  @DoNotStrip
  fun getAsset(id: String, callback: GetAssetsCallback) {
    scope.launch {
      val contentResolver = context.contentResolver
      val jsonArray = contentResolver.singleQuery(
        EXTERNAL_CONTENT_URI,
        context,
        JSONObject(),
        id
      )
      if (jsonArray.length() == 0) {
        return@launch callback.onChange("")
      }
      val media = jsonArray.getJSONObject(0)
      MediaLibraryUtils.getMediaLocation(media, contentResolver)
      callback.onChange(media.toString())
    }
  }

  @DoNotStrip
  fun saveToLibrary(params: String, callback: GetAssetsCallback) {
    scope.launch {
      val input = params.asJsonInput()
      MedialLibraryCreateAsset.saveToLibrary(input, context) { error, id ->
        if (error != null) {
          callback.onChange(error)
        } else {
          getAsset(id!!, callback)
        }
      }
    }
  }

  companion object {
    var EXTERNAL_CONTENT_URI: Uri = MediaStore.Files.getContentUri("external")
    var dateAdded = MediaStore.Files.FileColumns.DATE_ADDED
  }
}