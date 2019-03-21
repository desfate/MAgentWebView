package com.webview.client.magentwebview

import android.content.Intent
import android.os.Bundle
import android.view.View


import com.just.agentweb.MiddlewareWebClientBase
import com.webview.client.magentwebview.sonic.SonicImpl
import com.webview.client.magentwebview.sonic.SonicJavaScriptInterface
import com.webview.client.magentwebview.sonic.SonicJavaScriptInterface.PARAM_CLICK_TIME


/**
 * Created by cenxiaozhong on 2017/12/18.
 *
 * If you wanna use VasSonic to fast open first page , please
 * follow as sample to update your code;
 */

class VasSonicFragment : AgentWebFragment() {

    private var mSonicImpl: SonicImpl? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 1. 首先创建SonicImpl
        mSonicImpl = SonicImpl(this.arguments!!.getString("url_key"), this.context)
        // 2. 调用 onCreateSession
        mSonicImpl!!.onCreateSession()
        //3. 创建AgentWeb ，注意创建AgentWeb的时候应该使用加入SonicWebViewClient中间件
        super.onViewCreated(view, savedInstanceState) // 创建 AgentWeb 注意的 go("") 传入的 mUrl 应该null 或者""
        //4. 注入 JavaScriptInterface
        mAgentWeb.getJsInterfaceHolder().addJavaObject(
            "sonic",
            SonicJavaScriptInterface(
                mSonicImpl!!.sonicSessionClient,
                Intent().putExtra(PARAM_CLICK_TIME, arguments!!.getLong(PARAM_CLICK_TIME)).putExtra(
                    "loadUrlTime",
                    System.currentTimeMillis()
                )
            )
        )
        //5. 最后绑定AgentWeb
        mSonicImpl!!.bindAgentWeb(mAgentWeb)

    }

    //在步骤3的时候应该传入给AgentWeb
    public override fun getMiddlewareWebClient(): MiddlewareWebClientBase {
        return mSonicImpl!!.createSonicClientMiddleWare()
    }

    //getUrl 应该为null
    override fun getUrl(): String? {
        return null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //销毁SonicSession
        if (mSonicImpl != null) {
            mSonicImpl!!.destrory()
        }
    }

    companion object {
        fun create(bundle: Bundle?): VasSonicFragment {

            val mVasSonicFragment = VasSonicFragment()
            if (bundle != null) {
                mVasSonicFragment.arguments = bundle
            }
            return mVasSonicFragment
        }
    }
}
