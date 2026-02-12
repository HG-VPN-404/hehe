package com.ncorti.kotlin.template.app

import android.app.Dialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import okhttp3.*
import java.io.IOException

// --- DATA MODEL ---
data class ApiResponse(val status: String, val data: List<FileItem>?)
data class FileItem(
    val filename: String,
    val size_mb: String,
    val is_folder: Boolean,
    val thumb: String?,
    val links: FileLinks?
)
data class FileLinks(val browse: String?, val proxy: String?)

class MainActivity : AppCompatActivity() {

    private lateinit var etUrl: EditText
    private lateinit var btnLoad: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private val client = OkHttpClient()
    private val gson = Gson()
    private val BASE_API_URL = "https://apiku.pribadiku-230.workers.dev/?url="

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etUrl = findViewById(R.id.etUrl)
        btnLoad = findViewById(R.id.btnLoad)
        progressBar = findViewById(R.id.progressBar)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        btnLoad.setOnClickListener {
            val url = etUrl.text.toString()
            if (url.isNotEmpty()) fetchData(BASE_API_URL + url)
        }
    }

    private fun fetchData(fullUrl: String) {
        progressBar.visibility = View.VISIBLE
        client.newCall(Request.Builder().url(fullUrl).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { progressBar.visibility = View.GONE }
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonStr = response.body?.string()
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    try {
                        val apiResponse = gson.fromJson(jsonStr, ApiResponse::class.java)
                        if (apiResponse.status == "success" && apiResponse.data != null) {
                            recyclerView.adapter = FileAdapter(apiResponse.data)
                        } else {
                            Toast.makeText(this@MainActivity, "Folder Kosong / Error", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Error Parsing", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    inner class FileAdapter(private val list: List<FileItem>) : RecyclerView.Adapter<FileAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvFilename: TextView = itemView.findViewById(R.id.tvFilename)
            val tvSize: TextView = itemView.findViewById(R.id.tvSize)
            val imgThumb: ImageView = itemView.findViewById(R.id.imgThumb)
            val imgAction: ImageView = itemView.findViewById(R.id.imgAction)

            fun bind(item: FileItem) {
                tvFilename.text = item.filename
                
                // Logika Tampilan Icon & Info
                if (item.is_folder) {
                    tvSize.text = "Folder"
                    imgThumb.setImageResource(android.R.drawable.ic_menu_more)
                    imgAction.setImageResource(android.R.drawable.ic_input_add)
                } else {
                    val ext = item.filename.substringAfterLast('.', "").lowercase()
                    val type = when(ext) {
                        "jpg", "jpeg", "png", "webp" -> "Image"
                        "mp4", "mkv", "avi", "mov" -> "Video"
                        else -> "File"
                    }
                    tvSize.text = "${item.size_mb} MB • $type"
                    
                    if (!item.thumb.isNullOrEmpty()) {
                        Glide.with(itemView).load(item.thumb).into(imgThumb)
                    } else {
                        imgThumb.setImageResource(android.R.drawable.ic_menu_gallery)
                    }
                    
                    // Icon Action
                    if (type == "Video") imgAction.setImageResource(android.R.drawable.ic_media_play)
                    else if (type == "Image") imgAction.setImageResource(android.R.drawable.ic_menu_view)
                    else imgAction.setImageResource(android.R.drawable.stat_sys_download)
                }

                // Logika KLIK
                itemView.setOnClickListener {
                    if (item.is_folder) {
                        item.links?.browse?.let { fetchData(it) }
                    } else {
                        val link = item.links?.proxy
                        if (link != null) handleFileClick(item, link)
                        else Toast.makeText(itemView.context, "Link Error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(list[position])
        override fun getItemCount() = list.size
    }

    private fun handleFileClick(item: FileItem, url: String) {
        val ext = item.filename.substringAfterLast('.', "").lowercase()
        
        when {
            // VIDEO -> Buka PlayerActivity
            listOf("mp4", "mkv", "avi").contains(ext) -> {
                val options = arrayOf("Play in App", "Download")
                AlertDialog.Builder(this)
                    .setTitle(item.filename)
                    .setItems(options) { _, which ->
                        if (which == 0) {
                            val intent = Intent(this, PlayerActivity::class.java)
                            intent.putExtra("VIDEO_URL", url)
                            startActivity(intent)
                        } else {
                            downloadFile(url, item.filename)
                        }
                    }.show()
            }
            
            // GAMBAR -> Buka Dialog Preview
            listOf("jpg", "jpeg", "png", "webp").contains(ext) -> {
                showImageDialog(url)
            }
            
            // LAINNYA -> Download
            else -> downloadFile(url, item.filename)
        }
    }

    private fun showImageDialog(url: String) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        
        val imageView = ImageView(this)
        imageView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        
        Glide.with(this).load(url).into(imageView)
        dialog.setContentView(imageView)
        dialog.show()
    }

    private fun downloadFile(url: String, filename: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle(filename)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            Toast.makeText(this, "Downloading...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal Download", Toast.LENGTH_SHORT).show()
        }
    }
}