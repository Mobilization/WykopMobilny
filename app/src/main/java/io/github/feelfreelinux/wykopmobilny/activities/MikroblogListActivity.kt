package io.github.feelfreelinux.wykopmobilny.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import io.github.feelfreelinux.wykopmobilny.R
import io.github.feelfreelinux.wykopmobilny.utils.WykopApiManager

import io.github.feelfreelinux.wykopmobilny.adapters.MikroblogListAdapter
import io.github.feelfreelinux.wykopmobilny.objects.Entry
import io.github.feelfreelinux.wykopmobilny.objects.WykopApiData
import io.github.feelfreelinux.wykopmobilny.utils.EndlessScrollListener
import io.github.feelfreelinux.wykopmobilny.utils.parseEntry
import kotlinx.android.synthetic.main.activity_mikroblog.*
import org.json.JSONArray
import org.json.JSONObject

/*
 * This is base class for every mikroblog - entry showing activities
 * Extend it, and pass your data in overrided loadData() function
*/
abstract class MikroblogListActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {
    lateinit var wam : WykopApiManager
    lateinit var adapter : MikroblogListAdapter
    lateinit var endlessScrollListener : EndlessScrollListener
    lateinit var loadMoreAction : WykopApiManager.WykopApiAction
    var pagerEnabled = true
    var list = ArrayList<Entry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mikroblog)

        // Get WykopApiManager
        wam = WykopApiManager(intent.getSerializableExtra("wamData") as WykopApiData, this)

        swiperefresh.setOnRefreshListener(this)
        // Setup recycler view
        recyclerView.run {
            setHasFixedSize(true) // For better performance
            layoutManager = LinearLayoutManager(this@MikroblogListActivity)
        }
        if (pagerEnabled) {
            loadMoreAction = object : WykopApiManager.WykopApiAction {
                override fun success(json: JSONArray) {
                    (0 .. json.length()-1).mapTo(list) { parseEntry(json.getJSONObject(it)) }
                    recyclerView.adapter.notifyDataSetChanged()
                    swiperefresh.isRefreshing = false
                    showProgress(false)
                    // endlessScrollListener.resetState()
                }
            }

            endlessScrollListener = object : EndlessScrollListener(recyclerView.layoutManager as LinearLayoutManager) {
                override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView) {
                    loadData(page, loadMoreAction)
                }

            }

            recyclerView.addOnScrollListener(endlessScrollListener)
        }
        swiperefresh.isRefreshing = false
        showProgress(true)
        createAdapter()
    }

    fun createAdapter() {
        list.clear()

        if (pagerEnabled) {
            adapter = MikroblogListAdapter(list, true, wam.getData())
            recyclerView.adapter = adapter
            loadData(1, loadMoreAction)
            endlessScrollListener.resetState()
        } else {
            adapter = MikroblogListAdapter(list, false, wam.getData())
            recyclerView.adapter = adapter
            loadData(0, object : WykopApiManager.WykopApiAction{ // Child class will handle parsing data by itself
                override fun success(json: JSONObject) {
                    recyclerView.adapter.notifyDataSetChanged()
                    swiperefresh.isRefreshing = false
                    showProgress(false)
                }
            })
        }
    }

    abstract fun loadData(page: Int, action: WykopApiManager.WykopApiAction)

    fun showProgress(shouldShow : Boolean) {
        if (shouldShow)
            progressHeader.visibility = View.VISIBLE
        else progressHeader.visibility = View.GONE
    }

    override fun onRefresh() =
        createAdapter()

    override fun onBackPressed() {
        finish()
    }
}
