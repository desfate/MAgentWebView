package com.webview.client.magentwebview

import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import com.webview.client.magentwebview.sonic.SonicJavaScriptInterface.PARAM_CLICK_TIME

class AgentFragmentAcitivity: AppCompatActivity(){

    lateinit var mFragmentManager: FragmentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agentweb_fragment)
        mFragmentManager = supportFragmentManager
        val ft: FragmentTransaction = mFragmentManager.beginTransaction()
        var mBundle = Bundle()
        mBundle.putLong(PARAM_CLICK_TIME, intent.getLongExtra(PARAM_CLICK_TIME, -1L))
        mBundle.putString("url_key", "http://192.168.31.106:6029/trade_mobile")

        ft.add(
            R.id.container_framelayout,
            VasSonicFragment.create(mBundle),
            AgentWebFragment::class.java.name!!
        )

        ft.commit()
    }
}