package com.ncorti.kotlin.template.app

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.*
import java.io.IOException

// --- MODEL DATA (Sesuai JSON Kamu) ---
data class ApiResponse(
    val status: String,
    val data: List<FileItem>?
)

data class FileItem(
    val filename: String,
    val size_mb: String,
    val is_folder: Boolean,
    val thumb: String?,
    val links: FileLinks?
)

data class FileLinks(
    val browse: String?, // Link buat buka folder
    val proxy: String?   // Link buat download/stream
)

// --- MAIN ACTIVITY ---
class MainActivity : AppCompatActivity() {

    private lateinit var etUrl: EditText
    private lateinit var btnLoad: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private val client = OkHttpClient()
    private val gson = Gson()

    // Ganti URL ini dengan URL API Worker kamu yang asli
    private val BASE_API_URL = "https://apiku.pribadiku-230.workers.dev/?url="

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main) // Pastikan layout xml namanya activity_main

        // Inisialisasi View
        etUrl = findViewById(R.id.etUrl)
        btnLoad = findViewById(R.id.btnLoad)
        progressBar = findViewById(R.id.progressBar)
        recyclerView = findViewById(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)

        btnLoad.setOnClickListener {
            val url = etUrl.text.toString()
            if (url.isNotEmpty()) {
                fetchData(BASE_API_URL + url)
            } else {
                Toast.makeText(this, "Isi link dulu bro!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchData(fullUrl: String) {
        progressBar.visibility = View.VISIBLE
        
        val request = Request.Builder().url(fullUrl).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Gagal: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonStr = response.body?.string()
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    try {
                        val apiResponse = gson.fromJson(jsonStr, ApiResponse::class.java)
                        if (apiResponse.status == "success" && apiResponse.data != null) {
                            // Tampilkan data ke List
                            recyclerView.adapter = FileAdapter(apiResponse.data)
                        } else {
                            Toast.makeText(this@MainActivity, "Kosong atau Error", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "Error parsing JSON", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // --- ADAPTER LIST (Pengatur Tampilan Daftar) ---
    inner class FileAdapter(private val list: List<FileItem>) : RecyclerView.Adapter<FileAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvFilename: TextView = itemView.findViewById(R.id.tvFilename)
            val tvSize: TextView = itemView.findViewById(R.id.tvSize)
            val imgThumb: ImageView = itemView.findViewById(R.id.imgThumb)

            fun bind(item: FileItem) {
                tvFilename.text = item.filename
                tvSize.text = if (item.is_folder) "Folder" else "${item.size_mb} MB"

                // Load Gambar Thumb
                if (!item.thumb.isNullOrEmpty()) {
                    Glide.with(itemView).load(item.thumb).into(imgThumb)
                } else {
                    // Gambar default kalau folder/tidak ada thumb
                    val icon = if (item.is_folder) android.R.drawable.ic_menu_more else android.R.drawable.ic_menu_gallery
                    imgThumb.setImageResource(icon)
                }

                // KLIK ITEM
                itemView.setOnClickListener {
                    if (item.is_folder) {
                        // Kalau Folder -> Buka isi folder (Recursive)
                        item.links?.browse?.let { nextLink -> fetchData(nextLink) }
                    } else {
                        // Kalau File -> Tanya mau Stream atau Download
                        showActionDialog(item)
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_file, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(list[position])
        }

        override fun getItemCount() = list.size
    }

    // --- DIALOG PILIHAN (STREAM / DOWNLOAD) ---
    private fun showActionDialog(item: FileItem) {
        val options = arrayOf("Nonton (Stream)", "Download")
        val builder = AlertDialog.Builder(this)
        builder.setTitle(item.filename)
        builder.setItems(options) { _, which ->
            val link = item.links?.proxy
            if (link != null) {
                when (which) {
                    0 -> playVideo(link) // Stream
                    1 -> downloadFile(link, item.filename) // Download
                }
            } else {
                Toast.makeText(this, "Link error bro", Toast.LENGTH_SHORT).show()
            }
        }
        builder.show()
    }

    // Fungsi Stream (Buka Player HP)
    private fun playVideo(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(url), "video/*")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Gak ada aplikasi pemutar video bro", Toast.LENGTH_SHORT).show()
        }
    }

    // Fungsi Download (Pakai Download Manager Android)
    private fun downloadFile(url: String, filename: String) {
        try {
            val request = DownloadManager.Request(Uri.parse(url))
            request.setTitle(filename)
            request.setDescription("Downloading...")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            
            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            Toast.makeText(this, "Sedang mendownload...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal download: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}