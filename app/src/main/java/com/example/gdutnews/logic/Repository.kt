package com.example.gdutnews.logic

import androidx.lifecycle.liveData
import com.example.gdutnews.GDUTNewsApplication
import com.example.gdutnews.logic.network.GDUTNewsNetwork
import com.example.gdutnews.util.HiddenInputHelper
import kotlinx.coroutines.Dispatchers
import okhttp3.FormBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import kotlin.coroutines.CoroutineContext

object Repository {

    fun initSession() = fire(Dispatchers.IO) {
        val responseFirst = GDUTNewsNetwork.request("http://news.gdut.edu.cn/")
        // 获取Cookie
        val session = responseFirst.headers.values("Set-Cookie")[0]
        val sessionID = session.substring(0, session.indexOf(";"))
        GDUTNewsApplication.session = sessionID
        // 获取隐藏域
        val data = responseFirst.body?.string()
        val hidden = data?.let { HiddenInputHelper.getHiddenInfo(it) }
        var __EVENTVALIDATION= ""
        var __VIEWSTATE = ""
        if (hidden != null ){
            __VIEWSTATE = hidden.__VIEWSTATE
            __EVENTVALIDATION = hidden.__EVENTVALIDATION
        }
        // 模拟登录
        val formBody = FormBody.Builder()
            .add("__VIEWSTATE", __VIEWSTATE)
            .add("__EVENTVALIDATION", __EVENTVALIDATION)
            .add("ctl00\$ContentPlaceHolder1\$userEmail", "gdutnews")
            .add("ctl00\$ContentPlaceHolder1\$userPassWord", "newsgdut")
            .add("ctl00\$ContentPlaceHolder1\$CheckBox1", "on")
            .add("ctl00\$ContentPlaceHolder1\$Button1", "登录")
            .build()
        GDUTNewsNetwork.request(
            "http://news.gdut.edu.cn/UserLogin.aspx?preURL=http://news.gdut.edu.cn/default.aspx",
            sessionID,
            formBody
        )
        Result.success(sessionID)
    }

    fun getCategoryPage(category: String, formBody: FormBody? = null, searchAddress: String? = null) =
        fire(Dispatchers.IO) {
            var response: String? = null
            if (formBody != null) {
                if (searchAddress == null) {
                    response = GDUTNewsNetwork.requestReturnString(
                        "http://news.gdut.edu.cn/ArticleList.aspx?category=$category",
                        GDUTNewsApplication.session,
                        formBody
                    )
                }else{
                    response = GDUTNewsNetwork.requestReturnString(searchAddress, GDUTNewsApplication.session, formBody)
                }
            } else {
                response = GDUTNewsNetwork.requestReturnString(
                    "http://news.gdut.edu.cn/ArticleList.aspx?category=$category",
                    GDUTNewsApplication.session
                )
            }
            Result.success(response)
        }

    fun getPage(id: String) = fire(Dispatchers.IO){
        val response = GDUTNewsNetwork.requestReturnString("http://news.gdut.edu.cn/ViewArticle.aspx?articleid=$id", GDUTNewsApplication.session)
        Result.success(response)
    }

    fun getNormalPage(url: String, session: String = GDUTNewsApplication.session) = fire(Dispatchers.IO){
        val response = GDUTNewsNetwork.requestReturnString(url, session)
        Result.success(response)
    }

    private fun <T> fire(context: CoroutineContext, block: suspend () -> Result<T>) =
        liveData<Result<T>>(context) {
            val result = try {
                block()
            } catch (e: Exception) {
                Result.failure<T>(e)
            }
            emit(result)
        }
}