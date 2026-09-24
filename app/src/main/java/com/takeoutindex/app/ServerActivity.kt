package com.takeoutindex.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.takeoutindex.app.databinding.ActivityServerBinding

/**
 * Server management: the first screen when no server is configured, and
 * reachable later from the in-app Settings link. Lets the user enter an address
 * once, save several, switch the active one, and delete stale entries.
 *
 * Only the address is handled here. Username and password are entered on the
 * server's own login page inside the WebView; the session then persists.
 */
class ServerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityServerBinding
    private lateinit var store: ServerStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityServerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        store = ServerStore(this)

        binding.connectBtn.setOnClickListener { connect() }
        refreshList()
    }

    private fun connect() {
        val url = binding.urlInput.text?.toString()?.trim().orEmpty()
        if (url.isEmpty()) {
            Toast.makeText(this, "Enter a server address", Toast.LENGTH_SHORT).show()
            return
        }
        val label = binding.labelInput.text?.toString()?.trim().orEmpty()
        store.saveAndActivate(label, url)
        goToMain()
    }

    private fun refreshList() {
        val servers = store.list()
        binding.savedLabel.visibility = if (servers.isEmpty()) View.GONE else View.VISIBLE

        val labels = servers.map {
            val active = if (it.url == store.activeUrl()) "  ●" else ""
            "${it.label}\n${it.url}$active"
        }
        binding.savedList.adapter =
            ArrayAdapter(this, android.R.layout.simple_list_item_1, labels)

        binding.savedList.setOnItemClickListener { _, _, pos, _ ->
            store.setActive(servers[pos].url)
            goToMain()
        }
        binding.savedList.setOnItemLongClickListener { _, _, pos, _ ->
            confirmRemove(servers[pos])
            true
        }
    }

    private fun confirmRemove(server: Server) {
        AlertDialog.Builder(this)
            .setTitle("Remove server")
            .setMessage("${server.label}\n${server.url}")
            .setPositiveButton("Remove") { _, _ ->
                store.remove(server.url)
                refreshList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
