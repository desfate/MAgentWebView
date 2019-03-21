package com.webview.client.magentwebview

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.LinearLayout
import com.google.gson.Gson
import com.just.agentweb.*
import com.just.agentweb.download.AgentWebDownloader
import com.just.agentweb.download.DefaultDownloadImpl
import com.just.agentweb.download.DownloadListenerAdapter
import com.just.agentweb.download.DownloadingService
import com.webview.client.magentwebview.client.MiddlewareChromeClient
import com.webview.client.magentwebview.client.MiddlewareWebViewClient
import java.util.HashMap
import kotlinx.android.synthetic.main.toorbar_agentweb.*

open class AgentWebFragment :Fragment(){

    private val TAG by lazy { AgentWebFragment::class.java.simpleName }

    private var mDownloadingService: DownloadingService? = null

    lateinit var mAgentWeb: AgentWeb

    private val mGson = Gson()

    open fun getUrl(): String? {
        return ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_agentview, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAgentWeb = AgentWeb.with(this)//
            .setAgentWebParent(
                view as LinearLayout,
                -1,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            )//传入AgentWeb的父控件。
            .useDefaultIndicator(-1, 3)//设置进度条颜色与高度，-1为默认值，高度为2，单位为dp。
            .setAgentWebWebSettings(getSettings())//设置 IAgentWebSettings。
            .setWebViewClient(mWebViewClient)//WebViewClient ， 与 WebView 使用一致 ，但是请勿获取WebView调用setWebViewClient(xx)方法了,会覆盖AgentWeb DefaultWebClient,同时相应的中间件也会失效。
            .setWebChromeClient(mWebChromeClient) //WebChromeClient
            .setPermissionInterceptor(mPermissionInterceptor) //权限拦截 2.0.0 加入。
            .setSecurityType(AgentWeb.SecurityType.STRICT_CHECK) //严格模式 Android 4.2.2 以下会放弃注入对象 ，使用AgentWebView没影响。
            .setAgentWebUIController(UIController(activity)) //自定义UI  AgentWeb3.0.0 加入。
            .setMainFrameErrorView(
                R.layout.agentweb_error_page,
                -1
            ) //参数1是错误显示的布局，参数2点击刷新控件ID -1表示点击整个布局都刷新， AgentWeb 3.0.0 加入。
            .useMiddlewareWebChrome(getMiddlewareWebChrome()) //设置WebChromeClient中间件，支持多个WebChromeClient，AgentWeb 3.0.0 加入。
            .useMiddlewareWebClient(getMiddlewareWebClient()) //设置WebViewClient中间件，支持多个WebViewClient， AgentWeb 3.0.0 加入。
            //                .setDownloadListener(mDownloadListener) 4.0.0 删除该API//下载回调
            //                .openParallelDownload()// 4.0.0删除该API 打开并行下载 , 默认串行下载。 请通过AgentWebDownloader#Extra实现并行下载
            //                .setNotifyIcon(R.drawable.ic_file_download_black_24dp) 4.0.0删除该api //下载通知图标。4.0.0后的版本请通过AgentWebDownloader#Extra修改icon
            .setOpenOtherPageWays(DefaultWebClient.OpenOtherPageWays.DISALLOW)//打开其他页面时，弹窗质询用户前往其他应用 AgentWeb 3.0.0 加入。
            .interceptUnkownUrl() //拦截找不到相关页面的Url AgentWeb 3.0.0 加入。
            .createAgentWeb()//创建AgentWeb。
            .ready()//设置 WebSettings。
            .go(getUrl()) //WebView载入


        iv_back.setOnClickListener {
            if (!mAgentWeb.back()) {
                this@AgentWebFragment.activity!!.finish()
            }
        }

        iv_finish.setOnClickListener{
            this@AgentWebFragment.activity!!.finish()
        }

        iv_more.setOnClickListener{

        }

    }

    /**
     * @return IAgentWebSettings
     */
    fun getSettings(): IAgentWebSettings<*>?{
        var settingAgentWeb: AgentWeb? = null
        val abs = object : AbsAgentWebSettings() {
            override fun bindAgentWebSupport(agentWeb: AgentWeb) {
                settingAgentWeb = agentWeb
            }

            /**
             * AgentWeb 4.0.0 内部删除了 DownloadListener 监听 ，以及相关API ，将 Download 部分完全抽离出来独立一个库，
             * 如果你需要使用 AgentWeb Download 部分 ， 请依赖上 compile 'com.just.agentweb:download:4.0.0 ，
             * 如果你需要监听下载结果，请自定义 AgentWebSetting ， New 出 DefaultDownloadImpl，传入DownloadListenerAdapter
             * 实现进度或者结果监听，例如下面这个例子，如果你不需要监听进度，或者下载结果，下面 setDownloader 的例子可以忽略。
             * @param webView
             * @param downloadListener
             * @return WebListenerManager
             */
            override fun setDownloader(
                webView: WebView,
                downloadListener: android.webkit.DownloadListener ?
            ): WebListenerManager {
                return super.setDownloader(
                    webView,
                    DefaultDownloadImpl
                        .create(
                            webView.context!! as Activity,
                            webView!!,
                            mDownloadListenerAdapter!!,
                            mDownloadListenerAdapter!!,
                            settingAgentWeb!!.getPermissionInterceptor()
                        )
                )
            }
        }
        return abs
    }

    /**
     * 更新于 AgentWeb  4.0.0
     */
    protected var mDownloadListenerAdapter: DownloadListenerAdapter = object : DownloadListenerAdapter() {

        /**
         *
         * @param url                下载链接
         * @param userAgent          UserAgent
         * @param contentDisposition ContentDisposition
         * @param mimetype           资源的媒体类型
         * @param contentLength      文件长度
         * @param extra              下载配置 ， 用户可以通过 Extra 修改下载icon ， 关闭进度条 ， 是否强制下载。
         * @return true 表示用户处理了该下载事件 ， false 交给 AgentWeb 下载
         */
        override fun onStart(
            url: String?,
            userAgent: String?,
            contentDisposition: String?,
            mimetype: String?,
            contentLength: Long,
            extra: AgentWebDownloader.Extra
        ): Boolean {
            LogUtils.i(TAG, "onStart:" + url!!)
            extra.setOpenBreakPointDownload(true) // 是否开启断点续传
                .setIcon(R.drawable.ic_file_download_black_24dp) //下载通知的icon
                .setConnectTimeOut(6000) // 连接最大时长
                .setBlockMaxTime(10 * 60 * 1000)  // 以8KB位单位，默认60s ，如果60s内无法从网络流中读满8KB数据，则抛出异常
                .setDownloadTimeOut(java.lang.Long.MAX_VALUE) // 下载最大时长
                .setParallelDownload(false)  // 串行下载更节省资源哦
                .setEnableIndicator(true)  // false 关闭进度通知
                .addHeader("Cookie", "xx") // 自定义请求头
                .setAutoOpen(true).isForceDownload = true // 强制下载，不管网络网络类型
            return false
        }

        /**
         *
         * 不需要暂停或者停止下载该方法可以不必实现
         * @param url
         * @param downloadingService  用户可以通过 DownloadingService#shutdownNow 终止下载
         */
        override fun onBindService(url: String?, downloadingService: DownloadingService?) {
            super.onBindService(url, downloadingService)
            mDownloadingService = downloadingService
            LogUtils.i(TAG, "onBindService:$url  DownloadingService:$downloadingService")
        }

        /**
         * 回调onUnbindService方法，让用户释放掉 DownloadingService。
         * @param url
         * @param downloadingService
         */
        override fun onUnbindService(url: String?, downloadingService: DownloadingService?) {
            super.onUnbindService(url, downloadingService)
            mDownloadingService = null
            LogUtils.i(TAG, "onUnbindService:" + url!!)
        }

        /**
         *
         * @param url  下载链接
         * @param loaded  已经下载的长度
         * @param length    文件的总大小
         * @param usedTime   耗时 ，单位ms
         * 注意该方法回调在子线程 ，线程名 AsyncTask #XX 或者 AgentWeb # XX
         */
        override fun onProgress(url: String?, loaded: Long, length: Long, usedTime: Long) {
            val mProgress = (loaded / java.lang.Float.valueOf(length.toFloat()) * 100).toInt()
            LogUtils.i(TAG, "onProgress:$mProgress")
            super.onProgress(url, loaded, length, usedTime)
        }

        /**
         *
         * @param path 文件的绝对路径
         * @param url  下载地址
         * @param throwable    如果异常，返回给用户异常
         * @return true 表示用户处理了下载完成后续的事件 ，false 默认交给AgentWeb 处理
         */
        override fun onResult(path: String?, url: String?, throwable: Throwable?): Boolean {
            if (null == throwable) { //下载成功
                //do you work
            } else {//下载失败

            }
            return false // true  不会发出下载完成的通知 , 或者打开文件
        }
    }


    /**
     *  webViewClient
     */
    protected var mWebViewClient: WebViewClient = object : WebViewClient() {

        private val timer = HashMap<String, Long>()

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            super.onReceivedError(view, request, error)
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            return shouldOverrideUrlLoading(view, request.url.toString() + "")
        }

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            return super.shouldInterceptRequest(view, request)
        }

        //
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {

            Log.i(TAG, "view:" + Gson().toJson(view.hitTestResult))
            Log.i(TAG, "mWebViewClient shouldOverrideUrlLoading:$url")
            //intent:// scheme的处理 如果返回false ， 则交给 DefaultWebClient 处理 ， 默认会打开该Activity  ， 如果Activity不存在则跳到应用市场上去.  true 表示拦截
            //例如优酷视频播放 ，intent://play?...package=com.youku.phone;end;
            //优酷想唤起自己应用播放该视频 ， 下面拦截地址返回 true  则会在应用内 H5 播放 ，禁止优酷唤起播放该视频， 如果返回 false ， DefaultWebClient  会根据intent 协议处理 该地址 ， 首先匹配该应用存不存在 ，如果存在 ， 唤起该应用播放 ， 如果不存在 ， 则跳到应用市场下载该应用 .
            return if (url.startsWith("intent://") && url.contains("com.youku.phone")) {
                true
            } else false
            /*else if (isAlipay(view, mUrl))   //1.2.5开始不用调用该方法了 ，只要引入支付宝sdk即可 ， DefaultWebClient 默认会处理相应url调起支付宝
			    return true;*/


        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap ?) {

            Log.i(TAG, "mUrl:" + url + " onPageStarted  target:" + getUrl())
            timer[url] = System.currentTimeMillis()
            if (url == getUrl()) {
                pageNavigator(View.GONE)
            } else {
                pageNavigator(View.VISIBLE)
            }

        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)

            if (timer[url] != null) {
                val overTime = System.currentTimeMillis()
                val startTime = timer[url]
                Log.i(TAG, "  page mUrl:" + url + "  used time:" + (overTime - startTime!!))
            }

        }
        /*错误页回调该方法 ， 如果重写了该方法， 上面传入了布局将不会显示 ， 交由开发者实现，注意参数对齐。*/
        /* public void onMainFrameError(AbsAgentWebUIController agentWebUIController, WebView view, int errorCode, String description, String failingUrl) {

            Log.i(TAG, "AgentWebFragment onMainFrameError");
            agentWebUIController.onMainFrameError(view,errorCode,description,failingUrl);

        }*/

        override fun onReceivedHttpError(
            view: WebView,
            request: WebResourceRequest,
            errorResponse: WebResourceResponse
        ) {
            super.onReceivedHttpError(view, request, errorResponse)

            //			Log.i(TAG, "onReceivedHttpError:" + 3 + "  request:" + mGson.toJson(request) + "  errorResponse:" + mGson.toJson(errorResponse));
        }

        override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
            super.onReceivedError(view, errorCode, description, failingUrl)

            //			Log.i(TAG, "onReceivedError:" + errorCode + "  description:" + description + "  errorResponse:" + failingUrl);
        }
    }

    protected var mWebChromeClient: WebChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            //  super.onProgressChanged(view, newProgress);
            Log.i(TAG, "onProgressChanged:$newProgress  view:$view")
        }

        override fun onReceivedTitle(view: WebView, title: String) {
            var title = title
            super.onReceivedTitle(view, title)
            if (toolbar_title != null && !TextUtils.isEmpty(title)) {
                if (title.length > 10) {
                    title = title.substring(0, 10) + "..."
                }
            }
            toolbar_title.setText(title)
        }
    }

    protected var mPermissionInterceptor: PermissionInterceptor =
        PermissionInterceptor { url, permissions, action ->
            /**
             * PermissionInterceptor 能达到 url1 允许授权， url2 拒绝授权的效果。
             * @param url
             * @param permissions
             * @param action
             * @return true 该Url对应页面请求权限进行拦截 ，false 表示不拦截。
             */
            Log.i(TAG, "mUrl:" + url + "  permission:" + mGson.toJson(permissions) + " action:" + action)
            false
        }


    private fun pageNavigator(tag: Int) {
        iv_back.visibility = tag
        view_line.visibility = tag
    }

    /**
     * MiddlewareWebClientBase 是 AgentWeb 3.0.0 提供一个强大的功能，
     * 如果用户需要使用 AgentWeb 提供的功能， 不想重写 WebClientView方
     * 法覆盖AgentWeb提供的功能，那么 MiddlewareWebClientBase 是一个
     * 不错的选择 。
     *
     * @return
     */
    private var mMiddleWareWebClient: MiddlewareWebClientBase? = null
    private var mMiddleWareWebChrome: MiddlewareWebChromeBase? = null
    protected open fun getMiddlewareWebClient(): MiddlewareWebClientBase {
        this.mMiddleWareWebClient = object : MiddlewareWebViewClient() {
            /**
             *
             * @param view
             * @param url
             * @return
             */
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {

                if (url.startsWith("agentweb")) { // 拦截 url，不执行 DefaultWebClient#shouldOverrideUrlLoading
                    Log.i(TAG, "agentweb scheme ~")
                    return true
                }

                return if (super.shouldOverrideUrlLoading(view, url)) { // 执行 DefaultWebClient#shouldOverrideUrlLoading
                    true
                } else false
                // do you work
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
        return this.mMiddleWareWebClient as MiddlewareWebViewClient
    }

    protected fun getMiddlewareWebChrome(): MiddlewareWebChromeBase {
        this.mMiddleWareWebChrome = object : MiddlewareChromeClient() {

        }
        return this.mMiddleWareWebChrome as MiddlewareChromeClient
    }

}