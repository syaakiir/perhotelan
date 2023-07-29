package com.pnj.hotel.tamu

import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.storage.FirebaseStorage
import com.pnj.hotel.R
import java.io.File

class TamuAdapter(private val tamuList: ArrayList<Tamu>)  :
    RecyclerView.Adapter<TamuAdapter.TamuViewHolder>() {
    private lateinit var activity: AppCompatActivity
    class TamuViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nik: TextView = itemView.findViewById(R.id.TVLNik)
        val nama: TextView = itemView.findViewById(R.id.TVLNama)
        val jenis_kelamin: TextView = itemView.findViewById(R.id.TVLJenisKelamin)
        val img_tamu: ImageView = itemView.findViewById(R.id.IMLGambarTamu)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TamuViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.tamu_list_layout,parent,false)
        return TamuViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TamuViewHolder, position: Int) {
        val tamu: Tamu = tamuList[position]
        holder.nik.text = tamu.nik
        holder.nama.text = tamu.nama
        holder.jenis_kelamin.text = tamu.jenis_kelamin

        holder.itemView.setOnClickListener{
            activity = it.context as AppCompatActivity
            activity.startActivity(Intent(activity, EditTamuActivity::class.java).apply {
                putExtra("nik",tamu.nik.toString())
                putExtra("nama",tamu.nama.toString())
                putExtra("tgl_lahir",tamu.tgl_lahir.toString())
                putExtra("jenis_kelamin",tamu.jenis_kelamin.toString())
            })
        }

        val storageRef = FirebaseStorage.getInstance().reference.child("img_tamu/${tamu.nik}_${tamu.nama}.jpg")
        val localfile = File.createTempFile("tempImage","jpg")
        storageRef.getFile(localfile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
            holder.img_tamu.setImageBitmap(bitmap)
        }.addOnFailureListener {
            Log.e("foto ?","gagal")
        }
    }

    override fun getItemCount(): Int {
        return tamuList.size
    }
}

