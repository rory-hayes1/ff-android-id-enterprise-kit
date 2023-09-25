package com.example.myapplication

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.webkit.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.helper.widget.MotionEffect.TAG
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.FileProvider
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private var uri: Uri? = null
    private var takeDocumentPicture: ActivityResultLauncher<Uri>? = null
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    @Throws(IOException::class)
    private fun createImageFile(): File? {
        val timeStamp = SimpleDateFormat(
            "yyyy-MM-dd-HH-mm-ss-SS",
            Locale.getDefault()
        ).format(Date())
        val imageFileName = "IMG_" + timeStamp + "_"
        val storageDir: File? = this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )
        return image
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //WebView Object
        val myWebView: WebView = findViewById(R.id.webView)
        myWebView.webChromeClient = object : WebChromeClient() {

            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                Log.d("MyApplication", "${message.message()} -- From line " +
                        "${message.lineNumber()} of ${message.sourceId()}")
                return true
            }
        }
        //Enable Javascript

        myWebView.settings.javaScriptEnabled = true

        myWebView.settings.domStorageEnabled = true;
        myWebView.settings.databaseEnabled = true;
        //myWebView.settings.javaScriptCanOpenWindowsAutomatically = true;

        val userAgent = java.lang.String.format(
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.0.0 Mobile Safari/537.36"
        )
        myWebView.settings.userAgentString = userAgent;
        myWebView.settings.mediaPlaybackRequiresUserGesture = false;

        //Inject WebAppInterface methods into Web page by having Interface 'Android'
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(this))
            .build()
        //myWebView.webViewClient = LocalContentWebViewClient(assetLoader)
        myWebView.webViewClient = MyWebViewClient();

        myWebView.webChromeClient = MyWebChromeClient();

        myWebView.addJavascriptInterface(WebAppInterface(this), "Android")
        myWebView.loadUrl("https://172.20.10.5:5500/OneSDK_IDKit.html")

        takeDocumentPicture =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
                if (success) {
                    // The image was saved into the given Uri -> do something with it
                    val msg = "Image captured successfully at : $uri"
                    Log.v(TAG, msg)
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    filePathCallback?.onReceiveValue(arrayOf(uri!!))
                }
            }
    }

    private inner class MyWebViewClient : WebViewClient() {
        // To allow redirections
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            // Allow webview to redirect normaly. "false" indicates no override of the default behaviour
            return false
        }
        // To allow self signed https servers
        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler,
            error: SslError?
        ) {
            // Handle SSL errors here
            // For a self-signed certificate, you might choose to proceed anyway
            handler.proceed()
        }
    }

    private inner class MyWebChromeClient : WebChromeClient() {

        override fun onPermissionRequest(request: PermissionRequest?) {
            //super.onPermissionRequest(request)
            Log.i(TAG, "onPermissionRequest ${request?.resources}");
            val requestedResources = request!!.resources
            for (r in requestedResources) {
                if (r == PermissionRequest.RESOURCE_VIDEO_CAPTURE) {
                    request.grant(arrayOf(PermissionRequest.RESOURCE_VIDEO_CAPTURE))
                    break
                }
            }
        }

        override fun onPermissionRequestCanceled(request: PermissionRequest?) {
            super.onPermissionRequestCanceled(request)
            Log.i(TAG, "onPermissionRequestCanceled ${request?.resources}");
        }

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallbackParam: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            Log.v(TAG, "Show a file chooser")
            filePathCallback = filePathCallbackParam

            uri = createImageFile()?.let {
                FileProvider.getUriForFile(
                    this@MainActivity,
                    "com.example.ffintegrationandroidapp.fileprovider",
                    it
                )
            }
            takeDocumentPicture?.launch(uri);

            return true
        }
    }
}

private class LocalContentWebViewClient(private val assetLoader: WebViewAssetLoader) : WebViewClientCompat() {
    @RequiresApi(21)
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        return assetLoader.shouldInterceptRequest(request.url)
    }

    // to support API < 21
    override fun shouldInterceptRequest(
        view: WebView,
        url: String
    ): WebResourceResponse? {
        return assetLoader.shouldInterceptRequest(Uri.parse(url))
    }

    override fun onReceivedSslError(
        view: WebView?, handler: SslErrorHandler,
        error: SslError
    ) {

        handler.proceed()
    }
}

class WebAppInterface(private val mContext: Context) {
    /** Show a toast from the web page  */
    @JavascriptInterface
    fun showToast(toast: String) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show()
    }

    fun copyToClipboard(text: String?) {
        val clipboard: ClipboardManager =
            getSystemService(mContext,ClipboardManager::class.java) as ClipboardManager
        val clip = ClipData.newPlainText("demo", text)
        clipboard.setPrimaryClip(clip)
    }

}
