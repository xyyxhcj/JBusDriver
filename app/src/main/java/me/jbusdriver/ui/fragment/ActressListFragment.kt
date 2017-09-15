package me.jbusdriver.ui.fragment

import android.os.Bundle
import android.support.v4.util.ArrayMap
import android.support.v7.widget.OrientationHelper
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.cfzx.utils.CacheLoader
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import jbusdriver.me.jbusdriver.R
import me.jbusdriver.common.*
import me.jbusdriver.http.JAVBusService
import me.jbusdriver.mvp.bean.ActressInfo
import me.jbusdriver.mvp.bean.ILink
import me.jbusdriver.mvp.bean.SearchLink
import me.jbusdriver.mvp.bean.SearchWord
import me.jbusdriver.mvp.presenter.ActressLinkPresenterImpl
import me.jbusdriver.mvp.presenter.LinkAbsPresenterImpl
import me.jbusdriver.ui.activity.MovieListActivity
import me.jbusdriver.ui.activity.SearchResultActivity
import me.jbusdriver.ui.adapter.ActressInfoAdapter
import me.jbusdriver.ui.data.AppConfiguration
import me.jbusdriver.ui.data.DataSourceType
import me.jbusdriver.ui.data.collect.LinkCollector

class ActressListFragment : LinkableListFragment<ActressInfo>() {
    private val link by lazy { arguments.getSerializable(C.BundleKey.Key_1)  as? ILink ?: error("no link data ") }
    private val isSearch by lazy { link is SearchLink && activity != null && activity is SearchResultActivity }

    override val layoutManager: RecyclerView.LayoutManager  by lazy { StaggeredGridLayoutManager(viewContext.spanCount, OrientationHelper.VERTICAL) }
    override val adapter by lazy {
        ActressInfoAdapter(rxManager).apply {
            setOnItemClickListener { adapter, _, position ->
                (adapter.data.getOrNull(position) as? ActressInfo)?.let {
                    MovieListActivity.start(viewContext, it)
                }
            }

        }
    }

    override fun createPresenter() = ActressLinkPresenterImpl(link)

    override fun initWidget(rootView: View) {
        super.initWidget(rootView)
    }

    override fun initData() {
        if (isSearch) {
            RxBus.toFlowable(SearchWord::class.java).subscribeBy({ sea ->
                (mBasePresenter as? LinkAbsPresenterImpl<*>)?.let {
                    (it.linkData as SearchLink).query = sea.query
                    it.onRefresh()
                }
            }).addTo(rxManager)
        }
    }

    private var collectMenu: MenuItem? = null
    private var removeCollectMenu: MenuItem? = null
    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        menu?.findItem(R.id.action_show_all)?.isVisible = false
        if (isSearch) {
            val isCollect = LinkCollector.has(link as SearchLink)
            collectMenu = menu?.add(Menu.NONE, R.id.action_add_movie_collect, 10, "收藏")?.apply {
                setIcon(R.drawable.ic_star_border_white_24dp)
                setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                isVisible = !isCollect
            }
            removeCollectMenu = menu?.add(Menu.NONE, R.id.action_remove_movie_collect, 10, "取消收藏")?.apply {
                setIcon(R.drawable.ic_star_white_24dp)
                setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
                isVisible = isCollect
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_add_movie_collect -> {
                //收藏
                KLog.d("收藏")
                if (LinkCollector.addToCollect(link)) {
                    collectMenu?.isVisible = false
                    removeCollectMenu?.isVisible = true
                }
            }
            R.id.action_remove_movie_collect -> {
                //取消收藏
                KLog.d("取消收藏")
                if (LinkCollector.removeCollect(link)) {
                    collectMenu?.isVisible = true
                    removeCollectMenu?.isVisible = false
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }


    override fun gotoSearchResult(query: String) {
        (mBasePresenter as?  LinkAbsPresenterImpl<*>)?.let {
            if (isSearch) {
                viewContext.toast("新搜索 : $query")
                RxBus.post(SearchWord(query))
            } else {
                super.gotoSearchResult(query)
            }
        }
    }

    override val pageMode: Int = AppConfiguration.PageMode.Normal


    companion object {
        //需要处理搜索的特殊情况
        fun newInstance(link: ILink) = ActressListFragment().apply {
            arguments = Bundle().apply {
                putSerializable(C.BundleKey.Key_1, link)
            }
        }

        fun newInstance(type: DataSourceType) = ActressListFragment().apply {
            val urls = CacheLoader.acache.getAsString(C.Cache.BUS_URLS)?.let { AppContext.gson.fromJson<ArrayMap<String, String>>(it) } ?: arrayMapof()
            val url = urls[type.key] ?: JAVBusService.defaultFastUrl + "/actresses"
            arguments = Bundle().apply {
                putSerializable(C.BundleKey.Key_1, object : ILink {
                    override val link: String = url
                })
            }
        }

    }

    override fun insertDatas(pos: Int, datas: List<*>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun moveTo(pos: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}